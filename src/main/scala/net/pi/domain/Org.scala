package net.pi.domain

import net.pi.commons.Commons._
import net.pi.matchingutils.MatchingUtils
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

import scala.util.matching.Regex

/**
  * Created by farid on 18/04/16.
  */
case class Org(orgName: String, orgId:String, hostnames: List[String], nameWOStopWords:Option[String]=None,
               contactInfo: Option[ContactInfo]=None, processedName :String, nameRegEx:Regex,
               processedTokensName:List[String] = Nil, processedTokensNameRegEx:List[Regex]= Nil){

  def canEqual(a: Any) = a.isInstanceOf[Org]

  override def equals(that: scala.Any): Boolean =
    that match {
      case that: Org => that.canEqual(this) && this.orgId == that.orgId
      case _ => false
    }

  override def hashCode(): Int = orgId.hashCode
}


object Org {


  def createOrg(orgId: String , name: String,
                websites:List[String], contactInfo: Option[ContactInfo]= None):Org={

    val orgNameTrimmed = name.trim
    //Processed
    val processedName = MatchingUtils.escapeString(orgNameTrimmed, allowedChars)
    val nameRegEx = ("(^|.*\\s)" + processedName + "(\\s.*|$)").r

    val hostnames = websites.map(getHostname).map(_.trim)

    Org(orgId = orgId, orgName = orgNameTrimmed, hostnames = hostnames , contactInfo = contactInfo, processedName = processedName,
      nameRegEx = nameRegEx)

  }

  implicit object OrgReader extends BSONDocumentReader[Org] {

    def read(doc: BSONDocument): Org = {
      val orgId = doc.getAs[String]("entity_id").get
      val name = doc.getAs[String]("entity_name").get
      val websites:List[String]=List(doc.getAs[String]("website"), doc.getAs[String]("websiteRedirect")).flatten
      val contactInfo = doc.getAs[ContactInfo]("contact_info")

      createOrg(orgId=orgId, name =name, websites= websites)

    }
  }
}