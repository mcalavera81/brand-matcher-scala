package net.pi.matching

import MatchingUtils._
import net.pi.domain.{MatchResult, _}


/**
  * Created by farid on 18/04/16.
  */
object StrategiesDefinitions {


  sealed abstract class Strategies(val strategy: (Stream[MatchResult],Hotel) => AggregatedBrandingMatchResults)

  case object FIRST_MATCH extends Strategies(findFirstMatch _)
  case object SCORING extends Strategies(findWithScoring _)

  val strategiesDefinitionsMap = Map("FIRST_MATCH"->FIRST_MATCH,"SCORING"->SCORING)


  def findFirstMatch(matchingResults:Stream[MatchResult] ,hotel:Hotel): AggregatedBrandingMatchResults ={
    val matchResult = matchingResults.find(_.score > 0).getOrElse(buildMatchResult(hotel = hotel,score = 0))

    AggregatedBrandingMatchResults(hotel)(matchResult)

  }

  def findWithScoring(rawMatchingResults:Stream[MatchResult], hotel:Hotel):AggregatedBrandingMatchResults = {
    val successfulMatchingResults = rawMatchingResults.filter(_.score > 0).toList


    successfulMatchingResults match {
      case Nil =>
        val matchResult = buildMatchResult(hotel = hotel,score = 0)
        AggregatedBrandingMatchResults(hotel)(matchResult)

      case _ =>
        def matchKey: (MatchResult) => String = {
          x => x.brandName.getOrElse("&") + "|" + x.orgName.get
        }

        def getScore(matchResults:List[MatchResult]):Double =matchResults.map(_.score).sum

        def getMatchingTypeInfo(matchResults: List[MatchResult]) = matchResults.flatMap(_.matchingType)

        val matchingGroupedWithAggregatedScore: List[MatchResult] =successfulMatchingResults.groupBy(matchKey).mapValues({
          matchResults =>
            matchResults.head.copy(
              score = getScore(matchResults),
              matchingType = getMatchingTypeInfo(matchResults)
            )
        }).values.toList

        def sortResultsByScore(results: List[MatchResult]) = results.sortBy(_.score)(Ordering.Double.reverse)

        val results:List[MatchResult] = sortResultsByScore(matchingGroupedWithAggregatedScore)
        AggregatedBrandingMatchResults(hotel)(results :_*)


    }
  }


}

