package net.pi.service

import java.util.Optional

import net.pi.domain._
import net.pi.matching.BrandingMatcherComponent
import net.pi.matching.MatchingUtils._
import net.pi.persistence.PersistenceComponent
import net.pi.service.JavaOptionals._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by farid on 18/04/16.
  */
trait HotelBrandingServiceComponent {

  def hotelBrandingService : HotelBrandingService

  trait HotelBrandingService {

    def findBrand(name: Optional[String], website:Optional[String]):AggregatedBrandingMatchResults
    def findBrand(hotel:Hotel):AggregatedBrandingMatchResults
    def findBrandAndPersist(hotel:Hotel): WriteResults

  }

}


trait DefaultHotelBrandingServiceComponent extends  HotelBrandingServiceComponent{
  this: PersistenceComponent with BrandingMatcherComponent =>

  val hotelBrandingService = new  HotelBrandingServiceImpl


  class HotelBrandingServiceImpl extends  HotelBrandingService{

    lazy val brandMatchers = for {
      bmList <- brandingMatcherConfig.brandMatchers
      bm <- bmList
    }yield{
      bm(hotelBrandingDAO.getIdf)
    }

    lazy val orgMatchers = for {
      omList <- brandingMatcherConfig.orgMatchers
      om <- omList
    }yield{
      om(hotelBrandingDAO.getIdf)
    }

    override def findBrand(name: Optional[String], website: Optional[String]): AggregatedBrandingMatchResults = {
      val hotel =Hotel.createHotel(name= name.toOption, websiteOpt = website.toOption)
      findBrand(hotel)
    }

    override def findBrand(hotel: Hotel): AggregatedBrandingMatchResults = {

      val processedHotel = hotel.copy(hotelNameWOStopWordsOpt = hotel.processedNameOpt.map(_.split(" ").filterNot(hotelBrandingDAO.allStopWords.contains(_)).mkString(" ")))
      val matchingCandidates: Stream[MatchResult] =
        applyAllMatchers(brandMatchers,orgMatchers) (hotelBrandingDAO.brands,hotelBrandingDAO.orgs, processedHotel)


      val results: AggregatedBrandingMatchResults = brandingMatcherConfig.strategy(matchingCandidates, hotel)

      val filteredResults = results.copy(matchResults = results.matchResults.take(brandingMatcherConfig.resultsSize))
      filteredResults
    }

    override def findBrandAndPersist(hotel: Hotel): WriteResults = {
      val results = findBrand(hotel)
      hotelBrandingDAO.writeOutput(results)
    }
  }


}
