package net.pi.matching



import net.pi.domain.{Brand, Hotel, MatchResult, Org}

/**
  * Created by farid on 15/04/16.
  */
object MatchingUtils {


  def applyAllMatchers(brandMatchers: List[(Brand,Hotel)=>Option[MatchResult]], orgMatchers:List[(Org,Hotel)=>Option[MatchResult]])
                      (brands: Set[Brand], orgs: Set[Org], hotel: Hotel):Stream[MatchResult]={

    applyMatchers[Brand](brandMatchers)(brands, hotel) #::: applyMatchers[Org](orgMatchers)(orgs, hotel)


  }

  def applyMatchers[T](matchers: List[(T, Hotel) => Option[MatchResult]])(brands: Set[T], hotel: Hotel)
  : Stream[MatchResult] = {

    if (brands.isEmpty) Stream.empty[MatchResult]
    else {
      applyMatchersBlockToBrand(matchers)(brands.head, hotel) #::: applyMatchers(matchers)(brands.tail,hotel)
    }
  }


  def applyMatchersBlockToBrand[T](matchers: List[(T, Hotel) => Option[MatchResult]])
                               (brand: T, hotel: Hotel): Stream[MatchResult] = {
    matchers match {
      case matcher :: tailMatchersList =>
        matcher(brand, hotel) match{
          case Some(matchResult) => matchResult #:: applyMatchersBlockToBrand(tailMatchersList)(brand, hotel)
          case None =>  applyMatchersBlockToBrand(tailMatchersList)(brand, hotel)

        }
      case Nil => Stream.empty[MatchResult]
    }

  }


  def buildMatchResult(score:Double, hotel:Hotel, brand:Option[Brand]=None,
                       org:Option[Org]=None, matchingType: List[String]=Nil):MatchResult={

    new MatchResult(
      score=score,
      placeId = hotel.placeId,
      hotelName = hotel.name,
      hotelWebsite = hotel.websiteInfoOpt.map(_.website),
      brandName = brand.map(_.brandName),
      brandHostname = brand.toSet[Brand].flatMap(_.hostnames),
      orgName = org.map(_.orgName),
      hotelAddress = hotel.formattedAddress,
      orgHostname = org.toSet[Org].flatMap(_.hostnames),
      matchingType = matchingType
    )

  }

}
