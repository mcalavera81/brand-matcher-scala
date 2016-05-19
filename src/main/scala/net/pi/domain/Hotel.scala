package net.pi.domain

import net.pi.commons.Commons._
import net.pi.config.HotelBrandingConfig
import net.pi.matchingutils.MatchingUtils
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONObjectID}
import HotelBrandingConfig.MatchingConfig._

case class Hotel(name: Option[String], processedNameOpt: Option[String], placeId: Option[String]=None, formattedAddress: Option[String] =None,
                 hotelNameWOStopWordsOpt : Option[String] =None, websiteInfoOpt: Option[WebsiteInfo]=None, branding: Option[AggregatedBrandingMatchResults]=None)

case class AddressComponent(shortName:Option[String]=None, longName:Option[String]=None, types: Option[List[String]]=None)


object AddressComponent{
  implicit object AddressComponentReader extends BSONDocumentReader[AddressComponent] {
    def read(doc: BSONDocument): AddressComponent = {

      val shortName = doc.getAs[String]("shortName")
      val longName = doc.getAs[String]("longName")
      val types = doc.getAs[List[String]]("types")

      AddressComponent(shortName = shortName, longName = longName, types= types)
    }
  }
}




case class WebsiteInfo(website:String, hostname:String, tokens:String)
object Hotel{

  val tld =".ac|.ad|.ae|.af|.ag|.ai|.al|.am|.an|.ao|.aq|.ar|.as|.at|.au|.aw|.ax|.az|.ba|.bb|.bd|.be|.bf|" +
    ".bg|.bh|.bi|.bj|.bm|.bn|.bo|.bq|.br|.bs|.bt|.bv|.bw|.by|.bz|.ca|.cc|.cd|.cf|.cg|.ch|.ci|.ck|.cl|.cm|" +
    ".cn|.co|.cr|.cu|.cv|.cw|.cx|.cy|.cz|.de|.dj|.dk|.dm|.do|.dz|.ec|.ee|.eg|.eh|.er|.es|.et|.eu|.fi|.fj|" +
    ".fk|.fm|.fo|.fr|.ga|.gb|.gd|.ge|.gf|.gg|.gh|.gi|.gl|.gm|.gn|.gp|.gq|.gr|.gs|.gt|.gu|.gw|.gy|.hk|.hm|" +
    ".hn|.hr|.ht|.hu|.id|.ie|.il|.im|.in|.io|.iq|.ir|.is|.it|.je|.jm|.jo|.jp|.ke|.kg|.kh|.ki|.km|.kn|.kp|" +
    ".kr|.kw|.ky|.kz|.la|.lb|.lc|.li|.lk|.lr|.ls|.lt|.lu|.lv|.ly|.ma|.mc|.md|.me|.mg|.mh|.mk|.ml|.mm|.mn|" +
    ".mo|.mp|.mq|.mr|.ms|.mt|.mu|.mv|.mw|.mx|.my|.mz|.na|.nc|.ne|.nf|.ng|.ni|.nl|.no|.np|.nr|.nu|.nz|.om|" +
    ".pa|.pe|.pf|.pg|.ph|.pk|.pl|.pm|.pn|.pr|.ps|.pt|.pw|.py|.qa|.re|.ro|.rs|.ru|.rw|.sa|.sb|.sc|.sd|.se|" +
    ".sg|.sh|.si|.sj|.sk|.sl|.sm|.sn|.so|.sr|.ss|.st|.su|.sv|.sx|.sy|.sz|.tc|.td|.tf|.tg|.th|.tj|.tk|.tl|" +
    ".tm|.tn|.to|.tp|.tr|.tt|.tv|.tw|.tz|.ua|.ug|.uk|.us|.uy|.uz|.va|.vc|.ve|.vg|.vi|.vn|.vu|.wf|.ws|.ye|" +
    ".yt|.za|.zm|.zw"


  def createHotel(placeId: Option[String]=None, name: Option[String],
            websiteOpt:Option[String], formattedAddress: Option[String]=None,
                  branding: Option[AggregatedBrandingMatchResults]=None):Hotel = {


    val processedName = if(FILTER_CHARS) name.map(MatchingUtils.escapeString(_,allowedChars)) else name

    val  websiteInfo= websiteOpt.map{
      website =>
        val url =new java.net.URL(website)
        val urlRawData = String.join(" ", url.getHost, url.getPath)
        val tokens = urlRawData.replaceAll(s"($tld|-|_|/|www.|.com])"," ").replaceAll("\\s+", " ").trim
        val hostname = getHostname(website)
        WebsiteInfo(website, hostname, tokens)
    }

    Hotel(
      name=name,
      processedNameOpt=processedName,
      placeId = placeId,
      formattedAddress=formattedAddress,
      websiteInfoOpt = websiteInfo,
      branding=branding)


  }

  implicit object HotelReader extends BSONDocumentReader[Hotel] {
    def read(doc: BSONDocument): Hotel = {

      val placeId = doc.getAs[String]("_id")
      val name = doc.getAs[String]("name")

      val formattedAddress = doc.getAs[String]("formattedAddress")
      val websiteOpt = doc.getAs[String]("website")

      createHotel(placeId=placeId, name=name,websiteOpt=websiteOpt, formattedAddress=formattedAddress)

    }
  }
}

case class Batch(list:List[Hotel])

