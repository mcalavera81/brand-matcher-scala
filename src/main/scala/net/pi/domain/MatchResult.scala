package net.pi.domain

import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

import scala.beans.BeanProperty


/**
  * Created by farid on 18/04/16.
  */


case class AggregatedBrandingMatchResults(
                                   @BeanProperty maxScore :Double =0,
                                   @BeanProperty brandName: Option[String] = None,
                                   @BeanProperty orgName: Option[String] = None,
                                   @BeanProperty brandHostname : Set[String] = Set.empty[String],
                                   @BeanProperty orgHostname : Set[String] = Set.empty[String],
                                   @BeanProperty placeId:Option[String]=None,
                                   @BeanProperty hotelAddress: Option[String] =None,
                                   @BeanProperty hotelName: Option[String]=None,
                                   @BeanProperty hotelWebsite: Option[String]=None,
                                   @BeanProperty matchResults: List[MatchResult]
                                 ) extends Serializable



object AggregatedBrandingMatchResults{

  def apply(hotel: Hotel)(matchResults: MatchResult*)={

    val winningMatchResult = matchResults.head
    new AggregatedBrandingMatchResults(
      maxScore = winningMatchResult.score,
      brandName = winningMatchResult.brandName,
      brandHostname = winningMatchResult.brandHostname,
      orgHostname = winningMatchResult.orgHostname,
      orgName = winningMatchResult.orgName,
      matchResults= matchResults.toList,
      placeId = hotel.placeId,
      hotelName =  hotel.name,
      hotelAddress = hotel.formattedAddress,
      hotelWebsite = hotel.websiteInfoOpt.map(_.website)
    )
  }

  implicit object AggregatedMatchResultsReader extends BSONDocumentReader[AggregatedBrandingMatchResults] {
    def read(doc: BSONDocument): AggregatedBrandingMatchResults = {
      val maxScore = doc.getAs[Double]("maxScore").get
      val brandName = doc.getAs[String]("brandName")
      val orgName = doc.getAs[String]("orgName")

      AggregatedBrandingMatchResults(maxScore= maxScore, brandName= brandName, orgName=orgName,matchResults = List())
    }
  }


}


case class MatchResult(
                        score:Double,
                        hotelAddress: Option[String] =None,
                        hotelName: Option[String] =None,
                        hotelWebsite: Option[String]=None,
                        placeId:Option[String]=None,
                        brandName: Option[String]=None,
                        brandHostname: Set[String]=Set.empty[String],
                        orgName: Option[String]=None,
                        orgHostname: Set[String]=Set.empty[String],
                        matchingType: List[String]=Nil
                      )

