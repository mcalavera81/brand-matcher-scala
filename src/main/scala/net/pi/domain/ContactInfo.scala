package net.pi.domain

import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

/**
  * Created by farid on 18/04/16.
  */
case class ContactInfo(address:Option[String], fax:Option[String],
                       name:Option[String], phone:Option[String])

object ContactInfo{

  implicit object ContactInfoReader extends BSONDocumentReader[ContactInfo] {
    def read(doc: BSONDocument): ContactInfo = {
      val name = doc.getAs[String]("name")
      val address = doc.getAs[String]("address")
      val fax = doc.getAs[String]("fax")
      val phone = doc.getAs[String]("phone")

      ContactInfo(address=address ,fax=fax ,name=name ,phone=phone)
    }
  }
}

