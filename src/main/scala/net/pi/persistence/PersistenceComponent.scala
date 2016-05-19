package net.pi.persistence

import com.typesafe.scalalogging.LazyLogging
import net.pi.commons.Commons
import net.pi.config.HotelBrandingConfig.{MatchingConfig, PersistenceConfig}
import net.pi.domain._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, _}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * Created by farid on 18/04/16.
  */
trait PersistenceComponent {

  def hotelBrandingDAO: HotelBrandingDAO

  trait HotelBrandingDAO{

    type WordFrequencyMap = Map[String, Int]

    def prepareOutput()(implicit ec:ExecutionContext): Unit
    def init_brands_orgs_stopwords_wordFreq(implicit ec:ExecutionContext):BrandingInitialization
    def writeOutput(matchResults: AggregatedBrandingMatchResults)(implicit ec:ExecutionContext): WriteResults
    def findHotels(upTo: Int = Int.MaxValue, page:Int = 0)(implicit ec:ExecutionContext):Future[List[Hotel]]
    def brands :Set[Brand]
    def orgs :Set[Org]
    def allStopWords: Set[String]
    def getIdf:String=>Double
    def docCount:Int
    def updatedHotelModel:Boolean
    def finHotel(placeId: String):Option[Hotel]
    def cleanUp():Unit

  }

  trait CommonPersistence extends HotelBrandingDAO with LazyLogging{


    def updatedHotelModel : Boolean = PersistenceConfig.UPDATE_HOTEL_MODEL

    def docCount: Int = MatchingConfig.DOC_COUNT

    private def getIdf(wordFreqMap:Map[String, Int])(word:String) :Double={
      val docFreq =wordFreqMap.getOrElse(word,0)
      1+ Math.log(docCount.toDouble/(docFreq+1))
    }



    def createStopWords(brands:  List[Brand]): Set[String] ={

      val (brandsWords, orgsWords)=brands.foldLeft((ListBuffer.empty[String],ListBuffer.empty[String])){
        (acc, brand)=>
          (acc._1 ++ brand.processedBrandName.split(" ").map(_.trim),
            acc._2 ++ brand.organization.map(org=> org.processedName.split(" ").map(_.trim)).getOrElse(Array()))
      }


      val stopWordsBrands = brandsWords.groupBy(identity).mapValues(_.length).filter(elem=>elem._2 > 10).keySet
      val stopWordsOrgs = orgsWords.groupBy(identity).mapValues(_.length).filter(elem=>elem._2 > 20).keySet
      stopWordsBrands ++ stopWordsOrgs

    }


    def createWordFreqFile()={
      Commons.writeMapToFile("wordFreq",wordFrequencyHotels(Await.result(findHotels(),Duration.Inf)))
    }

    override def init_brands_orgs_stopwords_wordFreq(implicit ec:ExecutionContext):BrandingInitialization={

      prepareOutput()
      val rawBrands : List[Brand] = getDbBrands
      logger.info(s"Loaded ${rawBrands.length} brands")

      val allStopWords = createStopWords(rawBrands)
      val processedBrands = initializeBrands(rawBrands, allStopWords)
      val processedOrgs = initializeOrganizations(processedBrands, allStopWords)
      //
      //val wordFrequencyMap: WordFrequencyMap = wordFrequencyHotels(hotels)
      val wordFreqMap: WordFrequencyMap = Commons.readWordFreq("wordFreq")
      val idf = getIdf(wordFreqMap)_

      BrandingInitialization(processedBrands = processedBrands,
        processedOrgs = processedOrgs,
        allStopWords = allStopWords,
        idf = idf)

    }

    def getHotelInfoMap(aggregatedMatchResults: AggregatedBrandingMatchResults):Map[String,Any]={
      Map(
        "place_id"-> aggregatedMatchResults.placeId,
        "hotelAddress"->aggregatedMatchResults.hotelAddress,
        "hotelName"-> aggregatedMatchResults.hotelName,
        "hotelWebsite"->aggregatedMatchResults.hotelWebsite)
    }

    def getResultsMap(aggregatedMatchResults: AggregatedBrandingMatchResults):Map[String,Any]={
      val rootDocument=Map("maxScore" -> aggregatedMatchResults.maxScore)

      aggregatedMatchResults.maxScore match {
        case 0 => rootDocument
        case _ =>
          val matchResults = aggregatedMatchResults.matchResults.map {
            matchResult =>
              Map(
                "brand" -> matchResult.brandName,
                "org" -> matchResult.orgName,
                "matchingType" -> matchResult.matchingType,
                "score" -> matchResult.score)
          }

          rootDocument ++ Map("brandName" -> aggregatedMatchResults.brandName,
            "brandHostname" -> aggregatedMatchResults.brandHostname,
            "orgHostname" -> aggregatedMatchResults.orgHostname,
            "orgName" -> aggregatedMatchResults.orgName,
            "matchResults" -> matchResults
          )
      }

    }
    def getPersistentMap(aggregatedMatchResults: AggregatedBrandingMatchResults):Map[String,Any]={
        getHotelInfoMap(aggregatedMatchResults) ++ getResultsMap(aggregatedMatchResults)
    }

    private def wordFrequencyHotels(hotelList:List[Hotel]): WordFrequencyMap={

      def countsMerge(c1: WordFrequencyMap, c2: WordFrequencyMap): WordFrequencyMap = (c1 foldLeft c2) (updateCount)

      def updateCount(c: WordFrequencyMap, update: (String, Int)): WordFrequencyMap = {
        val (word, incr) = update
        c + (word -> (c.getOrElse(word, 0) + incr))
      }

      def countWords(hotel : Hotel): WordFrequencyMap = {
        def incrementCount(c: WordFrequencyMap, word: String): WordFrequencyMap = updateCount(c, (word, 1))

        hotel.processedNameOpt.fold(Map.empty[String, Int]) {
          processedName =>
            processedName.split(" ").map(_.trim).foldLeft(Map.empty[String, Int]) {
              (freqMap, word) => incrementCount(freqMap, word)
            }
        }

      }

      hotelList.par.map(countWords).reduce[WordFrequencyMap](countsMerge)
    }

    private def initializeBrands(rawBrands: List[Brand], allStopWords: Set[String]):Set[Brand] ={

      rawBrands.toSet[Brand].map{brand =>
        brand.copy(
          entityNameWOStopWords = Some(brand.processedBrandName.split(" ").filterNot(allStopWords.contains).mkString(" "))
        )}
    }

    private def initializeOrganizations(brands: Set[Brand], allStopWords: Set[String]): Set[Org]= {

      brands.flatMap{brand =>brand.organization.
        toList.map{org =>
          org.copy(
            nameWOStopWords = Some(org.processedName.split(" ").filterNot(allStopWords.contains).mkString(" ")),
            processedTokensName = brand.processedTokensName.toList,
            processedTokensNameRegEx= brand.nameProcessedTokensRegEx.toList
          )
        }
      }
    }

    def getDbBrands: List[Brand]
  }

}




