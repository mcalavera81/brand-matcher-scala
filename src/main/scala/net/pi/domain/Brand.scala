package net.pi.domain

import net.pi.commons.Commons._
import net.pi.matchingutils.MatchingUtils
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

import scala.util.matching.Regex

/**
  * Created by farid on 4/04/16.
  */

case class Brand(brandName: String, entityNameWOStopWords:Option[String]=None,
                 brandId:String, hostnames: List[String],
                 brandContactInfo: Option[ContactInfo], organization: Option[Org],
                 processedBrandName :String, brandNameRegEx:Regex,
                 processedTokensName:Array[String], nameProcessedTokensRegEx:Array[Regex]){

  def canEqual(a: Any) = a.isInstanceOf[Brand]

  override def equals(that: scala.Any): Boolean =
    that match {
      case that: Brand => that.canEqual(this) && this.brandId == that.brandId
      case _ => false
    }

  override def hashCode(): Int = brandId.hashCode
}

object Brand{


  def createBrand(brandId: String , brandName:String,
                  websites:List[String], brandContactInfo : Option[ContactInfo]= None,
                  organization: Option[Org], twitterUrl:Option[String]=None):Brand={


    val brandNameTrimmed = brandName.trim
    //Processed
    val processedBrandName = MatchingUtils.escapeString(brandNameTrimmed,allowedChars)

    val brandNameRegEx = ("(^|.*\\s)"+processedBrandName+"(\\s.*|$)").r

    val processedTokensName = processedBrandName.split("by").map(MatchingUtils.escapeString(_,allowedChars))
    val nameProcessedTokensRegEx = processedTokensName.map(processedToken => ("(^|.*\\s)"+processedToken+"(\\s.*|$)").r)

    val hostnames = websites.map(getHostname).map(_.trim)

    Brand(brandId=brandId,
      brandName=brandNameTrimmed,hostnames=hostnames, brandContactInfo= brandContactInfo,
      organization=organization,processedBrandName = processedBrandName, brandNameRegEx=brandNameRegEx,
      processedTokensName = processedTokensName, nameProcessedTokensRegEx= nameProcessedTokensRegEx)

  }

  implicit object EntityReader extends BSONDocumentReader[Brand] {

    def read(doc: BSONDocument): Brand = {

      val brandId = doc.getAs[String]("entity_id").get
      val twitterUrl = doc.getAs[String]("twitter_url")
      val brandName = doc.getAs[String]("entity_name").get

      val websites:List[String]=List(doc.getAs[String]("website"),
        doc.getAs[String]("websiteRedirect")).flatten

      val brandContactInfo = doc.getAs[ContactInfo]("contact_info")
      val organization = doc.getAs[Org]("parent")


      createBrand(brandId= brandId, twitterUrl = twitterUrl, brandName= brandName,
        websites = websites, organization = organization)

    }
  }
}







