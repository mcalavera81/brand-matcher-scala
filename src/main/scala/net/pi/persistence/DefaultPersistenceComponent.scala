package net.pi.persistence

import net.pi.domain._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, BSONDocument}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import BSONMap._
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.{MongoConnectionOptions, QueryOpts}

import scala.util.{Failure, Success, Try}


/**
  * Created by farid on 28/04/16.
  */
trait DefaultPersistenceComponent extends PersistenceComponent{
  import net.pi.config.HotelBrandingConfig.PersistenceConfig._


  val hotelBrandingDAO = new DefaultHotelBrandingDAO

  class DefaultHotelBrandingDAO extends CommonPersistence{

    val driver = new reactivemongo.api.MongoDriver

    val connection = driver.connection(List(servers), options = MongoConnectionOptions(connectTimeoutMS = 30000))
    val db = connection(database)
    val hotel_coll = db[BSONCollection](hotel_collection_name)
    val brand_coll = db[BSONCollection](brand_collection_name)
    val match_coll = db[BSONCollection](match_collection_name)

    val BrandingInitialization(brands, orgs, allStopWords, getIdf) = init_brands_orgs_stopwords_wordFreq


    override def prepareOutput()(implicit ec:ExecutionContext)={
      match_coll.drop()
    }

    override def writeOutput(matchResult: AggregatedBrandingMatchResults)(implicit ec:ExecutionContext): WriteResults ={
      mongoDbPersist(matchResult)
    }

    def insertMatch(hotelInfoMap: Map[String,Any], resultsMap: Map[String,Any]): Unit ={
      match_coll.insert(BSON.writeDocument(hotelInfoMap++resultsMap))
    }

    def setHotelBranding(placeId: String, resultsMap: Map[String,Any]): Boolean ={

      if(UPDATE_HOTEL_MODEL){
        val selector = BSONDocument("placeId"->placeId)
        val modifier = BSONDocument(
          "$set" -> BSONDocument("branding" -> BSON.writeDocument(resultsMap)))
        hotel_coll.update(selector=selector, update=modifier)
        true
      }else{
        false
      }
    }

    private def mongoDbPersist(aggregatedMatchResults: AggregatedBrandingMatchResults)(implicit ec:ExecutionContext):WriteResults ={

      val hotelInfoMap = getHotelInfoMap(aggregatedMatchResults)
      val resultsMap = getResultsMap(aggregatedMatchResults)
      insertMatch(hotelInfoMap, resultsMap)
      val updatedHotelModel = setHotelBranding(aggregatedMatchResults.placeId.get, resultsMap)
      WriteResults(updatedHotelModel = updatedHotelModel, aggregatedMatchResults = aggregatedMatchResults)
    }



    override def findHotels(upTo: Int = Int.MaxValue, page:Int = 0)(implicit ec:ExecutionContext):Future[List[Hotel]] ={
      logger.info(s"Finding hotels")

      val hotelQuery = BSONDocument()
      //val hotelQuery = BSONDocument("branding" -> BSONDocument("$exists" -> false))


      val hotelQueryBuilder = hotel_coll.find(hotelQuery).options(QueryOpts().batchSize(upTo).skip(page*upTo))
      val hotelCursor = hotelQueryBuilder.cursor[Hotel]()

      val hotelListFuture: Future[List[Hotel]] = hotelCursor.collect[List](upTo)
      hotelListFuture
    }

    override def getDbBrands: List[Brand] = {
      //Load Brand Data

      val brandQueryBuilder = brand_coll.find(BSONDocument())

      val brandCursor = brandQueryBuilder.cursor[Brand]()

      val brandListFuture: Future[List[Brand]] = brandCursor.collect[List]()
      val rawBrands : List[Brand] = Await.result(brandListFuture, Duration.Inf)
      rawBrands
    }

    override def finHotel(placeId: String): Option[Hotel] = {
      throw new UnsupportedOperationException("Not needed.")
    }

    override def cleanUp(): Unit = {
      match_coll.drop()
    }
  }

}