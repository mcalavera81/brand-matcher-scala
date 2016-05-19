package net.pi.matching

import net.pi.commons.Commons._
import net.pi.domain.{Brand, Hotel, MatchResult, Org}
import org.apache.commons.lang3.StringUtils
import MatchingUtils._
import net.pi.config.HotelBrandingConfig.MatchingConfig._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by farid on 13/04/16.
  */



object MatchersDefinitions {

  sealed abstract class BrandMatcher(val funct: (String=>Double)=>(Brand, Hotel) => Option[MatchResult])
  sealed abstract class OrgMatcher(val funct: (String=>Double)=>(Org, Hotel) => Option[MatchResult])

  case object BRAND_NAME_HOTEL_NAME_MATCHER extends BrandMatcher(funct=brandNameHotelNameMatcher(2)_)
  case object BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER extends BrandMatcher(funct=brandNameSplitByByHotelNameMatcher(2)_)
  case object BRAND_NAME_HOTEL_NAME_STOP_WORDS_JACCARD_MATCHER extends BrandMatcher(funct=brandNameHotelNameStopWordsJaccardMatcher(2) _)
  case object BRAND_HOSTNAME_HOTEL_HOSTNAME_MATCHER extends BrandMatcher(funct=brandHostnameHotelHostnameMatcher(4) _)
  case object BRAND_NAME_TOKENIZED_HOTEL_WEBSITE_MATCHER extends BrandMatcher(funct=brandNameTokenizedHotelWebsiteMatcher(1) _)
  case object BRAND_NAME_HOTEL_HOSTNAME_MATCHER extends BrandMatcher(funct=brandNameHotelHostnameMatcher(3) _)

  case object ORG_HOSTNAME_HOTEL_HOSTNAME_MATCHER extends OrgMatcher(funct=orgHostnameHotelHostname(3) _)
  case object ORG_NAME_HOTEL_NAME_MATCHER extends OrgMatcher(funct=orgNameHotelNameMatcher(2) _)
  case object ORG_FROM_BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER extends OrgMatcher(funct=orgFromBrandNameSplitByByHotelNameMatcher(2) _)
  case object ORG_NAME_HOTEL_NAME_STOPWORDS_JACCARD_MATCHER extends OrgMatcher(funct=orgNameHotelNameStopWordsJaccardMatcher(2) _)
  case object ORG_NAME_HOTEL_HOSTNAME_MATCHER extends OrgMatcher(funct=orgNameHotelHostnameMatcher(3) _)


  val brandMatchersDefinitionsMap = Map(
    "BRAND_NAME_HOTEL_NAME_MATCHER"->BRAND_NAME_HOTEL_NAME_MATCHER,
    "BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER"->BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER,
    "BRAND_NAME_HOTEL_NAME_STOP_WORDS_JACCARD_MATCHER"->BRAND_NAME_HOTEL_NAME_STOP_WORDS_JACCARD_MATCHER,
    "BRAND_HOSTNAME_HOTEL_HOSTNAME_MATCHER"->BRAND_HOSTNAME_HOTEL_HOSTNAME_MATCHER,
    "BRAND_NAME_TOKENIZED_HOTEL_WEBSITE_MATCHER"->BRAND_NAME_TOKENIZED_HOTEL_WEBSITE_MATCHER,
    "BRAND_NAME_HOTEL_HOSTNAME_MATCHER"->BRAND_NAME_HOTEL_HOSTNAME_MATCHER
  )

  val orgMatchersDefinitionsMap = Map(
    "ORG_HOSTNAME_HOTEL_HOSTNAME_MATCHER"->ORG_HOSTNAME_HOTEL_HOSTNAME_MATCHER,
    "ORG_NAME_HOTEL_NAME_MATCHER"->ORG_NAME_HOTEL_NAME_MATCHER,
    "ORG_FROM_BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER"->ORG_FROM_BRAND_NAME_SPLITBY_BY_HOTEL_NAME_MATCHER,
    "ORG_NAME_HOTEL_NAME_STOPWORDS_JACCARD_MATCHER"->ORG_NAME_HOTEL_NAME_STOPWORDS_JACCARD_MATCHER,
    "ORG_NAME_HOTEL_HOSTNAME_MATCHER"->ORG_NAME_HOTEL_HOSTNAME_MATCHER
  )

  def brandNameHotelNameMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                               (implicit ec:ExecutionContext): Option[MatchResult] = {

    for {
      hotelProcessedName <- hotel.processedNameOpt if !StringUtils.isEmpty(brand.processedBrandName)
      _ <- brand.brandNameRegEx.findFirstIn(hotelProcessedName)
    } yield {

      val wordsScore = brand.processedBrandName.split(" ").map(_.trim).map(getIdf).sum
      buildMatchResult(score= score*wordsScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandNameHotelName"))
    }

  }

  //Brand name split by 'by'
  def brandNameSplitByByHotelNameMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                                        (implicit ec:ExecutionContext):Option[MatchResult]={
    for {
      hotelProcessedName <- hotel.processedNameOpt
      regex = brand.nameProcessedTokensRegEx(0)
      _ <- regex.findFirstIn(hotelProcessedName)
    } yield {
      val wordsScore = brand.processedTokensName(0).split(" ").map(_.trim).map(getIdf).sum
      buildMatchResult(score= score*wordsScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandNameSplitByByHotelName"))
    }

  }

  def brandNameHotelNameStopWordsJaccardMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                                               (implicit ec:ExecutionContext): Option[MatchResult] ={
    for{
      hotelNameWOStopWords <- hotel.hotelNameWOStopWordsOpt
      if (!StringUtils.isEmpty(brand.processedBrandName)) &&
        (jaccard.compare( hotelNameWOStopWords, brand.entityNameWOStopWords.get) > JACCARD_THRESHOLD)
    }yield{

      val wordsScore = brand.entityNameWOStopWords.get.split(" ").map(_.trim).map(getIdf).sum

      buildMatchResult(score= score*wordsScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandNameHotelNameStopWordsJaccard"))
    }

  }


  //Levenshtein for Brand hostname vs hotel hostname
  def brandHostnameHotelHostnameMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                                       (implicit ec:ExecutionContext):Option[MatchResult]={
    (for{
      hotelWebsiteInfo <- hotel.websiteInfoOpt.toSeq
      brandHostnames <- brand.hostnames
      if levenshtein.compare(hotelWebsiteInfo.hostname, brandHostnames) > LEVENSHTEIN_THRESHOLD
    }yield{
      val hostnameScore = getIdf("#%#")
      buildMatchResult(score= score*hostnameScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandHostnameHotelHostname"))
    }).headOption

  }


  def brandNameHotelHostnameMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                                   (implicit ec:ExecutionContext): Option[MatchResult] ={


    val processedBrandNameForUrl = List(brand.processedBrandName.split(" ").mkString(""),brand.processedBrandName.split(" ").mkString("-"))

    (for{
      hotelWebsiteInfo <- hotel.websiteInfoOpt.toSeq  if(!StringUtils.isEmpty(brand.processedBrandName))
      brandNameForUrl <- processedBrandNameForUrl
      if levenshtein.compare(hotelWebsiteInfo.hostname, brandNameForUrl) > LEVENSHTEIN_THRESHOLD
    }yield{
      val hostnameScore = getIdf("#%#")
      buildMatchResult(score= score*hostnameScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandNameHotelHostname"))
    }).headOption


  }



  def brandNameTokenizedHotelWebsiteMatcher(score:Int)(getIdf:String=>Double)(brand: Brand, hotel: Hotel)
                                           (implicit ec:ExecutionContext):Option[MatchResult]={
    for{
      hotelWebsiteInfo <- hotel.websiteInfoOpt
      _ <- brand.brandNameRegEx.findFirstIn(hotelWebsiteInfo.tokens)
    }yield{

      val wordsScore = brand.processedBrandName.split(" ").map(_.trim).map(getIdf).sum
      val matchResult = buildMatchResult(score= score*wordsScore, hotel = hotel, brand = Some(brand), org = brand.organization,
        matchingType = List("BrandNameTokenizedHotelWebsite"))
      //writeOutput(matchResult)
      matchResult
    }
  }

  //Levenshtein for Organization hostname vs hotel hostname
  def orgHostnameHotelHostname(score:Int)(getIdf:String=>Double)(org: Org, hotel: Hotel)
                              (implicit ec:ExecutionContext):Option[MatchResult]= {

    (for{
      hotelWebsiteInfo <- hotel.websiteInfoOpt.toSeq
      org <- List(org)
      orgWebsite <- org.hostnames
      if levenshtein.compare(hotelWebsiteInfo.hostname, orgWebsite) > LEVENSHTEIN_THRESHOLD
    }yield {
      val hostnameScore = getIdf("#%#")
      buildMatchResult(score= score*hostnameScore, hotel = hotel, brand = None, org = Some(org),
            matchingType = List("OrgHostnameHotelHostname"))
    }).headOption

  }

  def orgNameHotelHostnameMatcher(score:Int)(getIdf:String=>Double)(org: Org, hotel: Hotel)
                                 (implicit ec:ExecutionContext): Option[MatchResult] ={


    val processedOrgNameForUrl = List(org.processedName.split(" ").mkString(""),org.processedName.split(" ").mkString("-"))

    (for{
      hotelWebsiteInfo <- hotel.websiteInfoOpt.toSeq
      orgNameForUrl <- processedOrgNameForUrl
      if levenshtein.compare(hotelWebsiteInfo.hostname, orgNameForUrl) > LEVENSHTEIN_THRESHOLD
    }yield{
      val hostnameScore = getIdf("#%#")
      buildMatchResult(score= score*hostnameScore, hotel = hotel, brand = None, org = Some(org),
        matchingType = List("OrgNameHotelHostname"))
    }).headOption


  }

  def orgNameHotelNameMatcher(score:Int)(getIdf:String=>Double)(org: Org, hotel: Hotel)
                             (implicit ec:ExecutionContext):Option[MatchResult]= {

    for {
      hotelProcessedName <- hotel.processedNameOpt if !StringUtils.isEmpty(org.processedName)
      _ <- org.nameRegEx.findFirstIn(hotelProcessedName)
    } yield {

      buildMatchResult(score= score, hotel = hotel, brand = None, org = Some(org),
        matchingType = List("OrgNameHotelName"))
    }

  }

  //Org from Brand name split by 'by'
  def orgFromBrandNameSplitByByHotelNameMatcher(score:Int)(getIdf:String=>Double)(org: Org, hotel: Hotel)
                                               (implicit ec:ExecutionContext):Option[MatchResult]= {
    for {
      hotelProcessedName <- hotel.processedNameOpt if org.processedTokensName.size > 1
      regex = org.processedTokensNameRegEx(1)
      _ <- regex.findFirstIn(hotelProcessedName)
    } yield {
      val wordsScore = org.processedTokensName(1).split(" ").map(_.trim).map(getIdf).sum

      buildMatchResult(score= score*wordsScore, hotel = hotel, brand = None, org = Some(org),
        matchingType = List("OrgFromBrandNameSplitByByHotelName"))
    }

  }

  def orgNameHotelNameStopWordsJaccardMatcher(score:Int)(getIdf:String=>Double)(org: Org, hotel: Hotel)
                                             (implicit ec:ExecutionContext):Option[MatchResult]= {
    for{
      hotelNameWOStopWords <- hotel.hotelNameWOStopWordsOpt
      if (!StringUtils.isEmpty(org.processedName)) &&
        (jaccard.compare(hotelNameWOStopWords, org.nameWOStopWords.get) > JACCARD_THRESHOLD)
    }yield {
      val wordsScore = org.nameWOStopWords.get.split(" ").map(_.trim).map(getIdf).sum

      buildMatchResult(score= score*wordsScore, hotel = hotel, brand = None, org = Some(org),
        matchingType = List("OrgNameHotelNameStopWordsJaccard"))
    }

  }


}


