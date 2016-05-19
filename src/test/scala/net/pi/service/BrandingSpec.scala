package net.pi.service

import java.awt.Choice
import java.util.Optional

import com.github.fakemongo.Fongo
import com.mongodb.util.JSON
import com.mongodb.{BasicDBObject, DBCollection, DBObject, FongoDB}
import net.pi.domain._
import net.pi.matching.{BrandingMatcherComponent, DefaultBrandingMatcherComponent}
import org.scalatest.{FeatureSpec, GivenWhenThen, ShouldMatchers, fixture}
import net.pi.persistence.PersistenceComponent

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future}
import net.pi.commons.Commons._
import net.pi.config.HotelBrandingConfig
import net.pi.matching.MatchersDefinitions._
import net.pi.matching.StrategiesDefinitions._
import org.bson.Document
import reactivemongo.api.commands.GetLastError.W

import scala.None
import scala.concurrent.duration.Duration
import scala.io.BufferedSource

/**
  * Created by farid on 20/04/16.
  */
class BrandingSpec extends FeatureSpec with ShouldMatchers with GivenWhenThen{

  val hotelBrandingFirstMatchFakeMongoServiceComponent = new DefaultHotelBrandingServiceComponent
    with FakeMongoPersistenceComponent
    with TestingBrandingMatcherFirstMatchComponent

  val hotelBrandingScoringFakeMongoServiceComponent = new DefaultHotelBrandingServiceComponent
    with FakeMongoPersistenceComponent
    with TestingBrandingMatcherScoringComponent


  feature("The Hotel Branding Service should try to find branding information for the hotel provided") {

    scenario("The hotel 'Fairfield Inn & Suites Phoenix Mesa' should get a matching brand of type 'BrandNameHotelName'") {

      Given("The hotel 'Fairfield Inn & Suites Phoenix Mesa'")
      val hotel = Hotel.createHotel(name = Some("Fairfield Inn & Suites Phoenix Mesa"),
        websiteOpt = Some("http://www.torrisdalecastle.com"))

      When("find brand is invoked on the hotel")
      val aggregatedMatchResult = hotelBrandingFirstMatchFakeMongoServiceComponent.hotelBrandingService.findBrand(hotel)

      Then("the hotel should be matched with a brand and a organization")

      aggregatedMatchResult.maxScore should be > 0d
      aggregatedMatchResult.matchResults.head.brandName should be(Some("Fairfield Inn"))
      aggregatedMatchResult.matchResults.head.orgName should be(Some("Marriott International, Inc."))
      aggregatedMatchResult.matchResults.head.matchingType should contain theSameElementsAs List("BrandNameHotelName")


    }


    scenario("The hotel 'Hotel NH Frankfurt Messe' should get a matching brand of type 'OrgHostnameHotelHostname'") {

      Given("The hotel 'Hotel NH Frankfurt Messe'")
      val hotel = Hotel.createHotel(name = Some("Hotel NH Frankfurt Messe"),
        websiteOpt = Some("http://www.nh-hotels.de/hotel/nh-frankfurt-messe?utm_source=google&utm_medium=maps&utm_campaign=googleplaces"))

      When("find brand is invoked on the hotel")
      val aggregatedMatchResult = hotelBrandingScoringFakeMongoServiceComponent.hotelBrandingService.findBrand(hotel)

      Then("the hotel should be matched with an organization but not a brand")

      aggregatedMatchResult.maxScore should be > 0d
      aggregatedMatchResult.matchResults.head.brandName should be(None)
      aggregatedMatchResult.matchResults.head.orgName should be(Some("NH Hoteles"))
      aggregatedMatchResult.matchResults.head.matchingType should contain theSameElementsAs List("OrgHostnameHotelHostname")

    }



    scenario("The hotel 'Quality Suites' should get a matching brand of type 'BrandNameHotelNameStopWordsJaccard',BrandNameTokenizedHotelWebsite'") {

      Given("The hotel 'Quality Suites'")
      val hotel = Hotel.createHotel(name = Some("Quality Suites"),
        websiteOpt = Some("https://www.choicehotels.com/oregon/keizer/quality-inn-hotels/or172?source=gglocaloz1"))

      When("findBrand is invoked on the hotel")
      val aggregatedMatchResult = hotelBrandingScoringFakeMongoServiceComponent.hotelBrandingService.findBrand(hotel)

      Then("the hotel should be matched with a brand and a organization")

      aggregatedMatchResult.maxScore should be > 0d
      aggregatedMatchResult.matchResults.head.matchingType should contain theSameElementsAs List("BrandNameHotelNameStopWordsJaccard")
      aggregatedMatchResult.matchResults.head.brandName should be(Some("Quality Inn"))
      aggregatedMatchResult.matchResults.head.orgName should be(Some("Choice Hotels International, Inc."))

    }
  }

  feature("The branding information should be persisted on the match-collection collection" +
    "and the hotel-collection") {
    scenario("The hotel 'Hilton Alger'  should be updatd with its branding information") {
      Given("the hotel with name 'Hilton Alger' with placeId 'ChIJdW8IdaWzjxIRpBrnv0rZuao'")
      val hotel = hotelBrandingScoringFakeMongoServiceComponent.hotelBrandingDAO.finHotel("ChIJdW8IdaWzjxIRpBrnv0rZuao")
      When("findBrandAndPersist  is invoked on the hotel")
      val writeResults = hotelBrandingScoringFakeMongoServiceComponent.hotelBrandingService.findBrandAndPersist(hotel.get)

      Then("the branding information should be in the hotel document")

      val hotelWithBranding = hotelBrandingScoringFakeMongoServiceComponent.hotelBrandingDAO.finHotel("ChIJdW8IdaWzjxIRpBrnv0rZuao")
      val branding = hotelWithBranding.get.branding.get

      branding.maxScore should be > 0d
      branding.brandName should be(Some("Hilton"))
      hotelWithBranding.get.branding.get.orgName should be(Some("Hilton Worldwide"))

    }
  }

}

trait TestingBrandingMatcherFirstMatchComponent extends  TestingBrandingMatcherComponent{
  val brandingMatcherConfig = new TestingBrandingMatcherConfig(FIRST_MATCH.strategy)
}

trait TestingBrandingMatcherScoringComponent extends  TestingBrandingMatcherComponent{
  val brandingMatcherConfig = new TestingBrandingMatcherConfig(SCORING.strategy)
}

trait TestingBrandingMatcherComponent extends  BrandingMatcherComponent{

  //val brandingMatcherConfig = new TestingBrandingMatcherFirstMatchConfig(FIRST_MATCH.strategy)

  class TestingBrandingMatcherConfig(val strategy: (Stream[MatchResult],Hotel) => AggregatedBrandingMatchResults) extends  BrandingMatcherConfig{


    private val block1 = List(BRAND_NAME_HOTEL_NAME_MATCHER,
      BRAND_NAME_HOTEL_NAME_STOP_WORDS_JACCARD_MATCHER,
      BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER)
    private val block2 = List(BRAND_HOSTNAME_HOTEL_HOSTNAME_MATCHER,
      BRAND_NAME_TOKENIZED_HOTEL_WEBSITE_MATCHER,
      BRAND_NAME_HOTEL_HOSTNAME_MATCHER)
    private val block3 = List(ORG_HOSTNAME_HOTEL_HOSTNAME_MATCHER,
      ORG_NAME_HOTEL_NAME_MATCHER,
      ORG_FROM_BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER,
      ORG_NAME_HOTEL_NAME_STOPWORDS_JACCARD_MATCHER,
      ORG_NAME_HOTEL_HOSTNAME_MATCHER)

    val brandMatchers= List(block1,block2).map(_.map(_.funct))
    val orgMatchers= List(block3).map(_.map(_.funct))

    //val strategy = FIRST_MATCH.strategy

    val resultsSize = 3

  }

}

trait FakeMongoPersistenceComponent extends PersistenceComponent {

  import net.pi.config.HotelBrandingConfig.PersistenceConfig._

  val hotelBrandingDAO = new FakeMongoHotelBrandingDAO

  class FakeMongoHotelBrandingDAO extends CommonPersistence{

    val fongo = new Fongo("Fake Mongo Server")
    val db:FongoDB = fongo.getDB(database)

    val brands_coll:DBCollection = db.getCollection(brand_collection_name)
    val hotels_coll:DBCollection = db.getCollection(hotel_collection_name)
    val match_coll:DBCollection = db.getCollection(match_collection_name)

    val BrandingInitialization(brands, orgs, allStopWords, getIdf) = init_brands_orgs_stopwords_wordFreq

    override def prepareOutput()(implicit ec: ExecutionContext): Unit = {
      insertFile(brands_coll,"brands.json")
      insertFile(hotels_coll,"hotels.json")
    }


    def stripOutOptions(container:Any):Any={
      container match{
        case map:Map[String,Any] => map.foldLeft(Map.empty[String,Any]){
          (map,elem) => map + (elem._1-> stripOutOptions(elem._2))
        }.asJava
        case Some(value) => stripOutOptions(value)
        case set: Set[_] => set.map(stripOutOptions).asJava
        case list: List[_] => list.map(stripOutOptions).asJava
        case None  => null
        case  value => value
      }
    }

    def getDbOBject(map :Map[String,Any]):DBObject={

      new BasicDBObject(stripOutOptions(map).asInstanceOf[java.util.Map[String,_]])

    }

    override def writeOutput(aggregatedMatchResults: AggregatedBrandingMatchResults)(implicit ec: ExecutionContext): WriteResults = {

      val hotelInfoMap = getHotelInfoMap(aggregatedMatchResults)
      val resultsMap = getResultsMap(aggregatedMatchResults)

      match_coll.insert(getDbOBject(hotelInfoMap++resultsMap))

      val writeResult =hotels_coll.update(
        new BasicDBObject("placeId", aggregatedMatchResults.placeId.get),
        new BasicDBObject("$set",
          new BasicDBObject("branding", getDbOBject(resultsMap))
        )
      )

      WriteResults(updatedHotelModel = true, aggregatedMatchResults)
    }


    override def finHotel(placeId: String): Option[Hotel] = {
      val hotelMongoDbOpt =Option.apply(hotels_coll.find(new BasicDBObject("placeId", placeId))
        .next().asInstanceOf[BasicDBObject])


      hotelMongoDbOpt.map{
        hotelMongoDb=>

          val placeId = Option.apply(hotelMongoDb.getString("placeId"))
          val name = Option.apply(hotelMongoDb.getString("name"))
          val website = Option.apply(hotelMongoDb.getString("website"))

          val branding= Option.apply(hotelMongoDb.get("branding").asInstanceOf[BasicDBObject])
            .map(brandingDb=>{

              val maxScore = brandingDb.getDouble("maxScore")
              val brandName = Option.apply(brandingDb.getString("brandName"))
              val orgName = Option.apply(brandingDb.getString("orgName"))

              AggregatedBrandingMatchResults(maxScore=maxScore, brandName = brandName, orgName =orgName, matchResults = List())

            })
          Hotel.createHotel(placeId=placeId, name=name, websiteOpt=website, branding=branding)
      }

    }

    override def findHotels(upTo: Int=Int.MaxValue,page:Int=0)(implicit ec: ExecutionContext): Future[List[Hotel]] = {
      throw new UnsupportedOperationException("Not supported for testing purposes.")
    }


    private  def insertFile(coll:DBCollection, filename:String)={
      val input = io.Source.fromInputStream(getClass.getResourceAsStream("/"+filename))
      using[BufferedSource,Unit](input){
        input=> for {
          line <- input.getLines.toList
        } yield {
          coll.insert(parseDBObject(line))
        }
      }
    }

    override def getDbBrands: List[Brand] = {


      brands_coll.find().iterator().asScala.toList.map(_.asInstanceOf[BasicDBObject]).map {
        dbObject =>
          val brandId = dbObject.getString("entity_id")
          val brandName = dbObject.getString("entity_name")
          val brandWebsite = dbObject.getString("website")
          val brandWebsiteRedirect = dbObject.getString("websiteRedirect")
          val orgId = dbObject.get("parent").asInstanceOf[BasicDBObject].getString("entity_id")



          val organization=Option.apply(orgId).map{
            orgId=>
              val orgName = dbObject.get("parent").asInstanceOf[BasicDBObject].getString("entity_name")
              val orgWebsite = dbObject.get("parent").asInstanceOf[BasicDBObject].getString("website")
              val orgWebsiteRedirect = dbObject.get("parent").asInstanceOf[BasicDBObject].getString("websiteRedirect")

              Org.createOrg(
                orgId =orgId,
                name = orgName,
                websites = List(Option.apply(orgWebsite),
                  Option.apply(orgWebsiteRedirect)).flatten)
          }

          Brand.createBrand(brandName = brandName,brandId=brandId,
            websites = List(Option.apply(brandWebsite), Option.apply(brandWebsiteRedirect)).flatten,
            organization = organization)
      }

    }


    private def parseDBObject(json: String):DBObject={
      JSON.parse(json).asInstanceOf[DBObject]
    }

    override def cleanUp(): Unit = ???

  }

}