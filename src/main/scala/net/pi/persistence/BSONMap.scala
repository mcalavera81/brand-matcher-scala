package net.pi.persistence

import java.io.StringWriter

import reactivemongo.bson.DefaultBSONHandlers.BSONUndefinedIdentity
import reactivemongo.bson.{BSON, BSONArray, BSONBoolean, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONDouble, BSONHandler, BSONInteger, BSONLong, BSONNull, BSONString, BSONUndefined, BSONValue, BSONWriter}

/**
  * Created by farid on 20/04/16.
  */

object BSONMap {


  //implicit val anyWriter = BSONMap.AnyWriter
  implicit val mapWriter: BSONDocumentWriter[Map[String, Any]] = BSONMap.MapWriter[Any]


  private implicit def MapWriter[Any](implicit vw: BSONWriter[Any, BSONValue]): BSONDocumentWriter[Map[String, Any]] = new BSONDocumentWriter[Map[String, Any]] {
    def write(map: Map[String, Any]): BSONDocument = {
      val elements = map.toStream.map { tuple =>
        tuple._1 -> vw.write(tuple._2)
      }
      BSONDocument(elements)
    }
  }

  private implicit def AnyWriter: BSONWriter[Any, BSONValue] = new BSONWriter[Any, BSONValue] {
    def write(any: Any): BSONValue = {
      any match {
        case Some(value)          => write(value)
        case value: String        => BSONString(value)
        case value: Double        => BSONDouble(value)
        case value: Int           => BSONInteger(value)
        case value: Boolean       => BSONBoolean(value)
        case value: Long          => BSONLong(value)
        case value: List[_]       => BSONArray(value.map(write))
        case value: Map[String,_] => mapWriter.write(value)
        case _ => BSONNull
      }

    }
  }

}

