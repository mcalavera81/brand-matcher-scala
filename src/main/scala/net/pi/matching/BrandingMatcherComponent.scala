package net.pi.matching



import java.util.Optional

import net.pi.config.HotelBrandingConfig
import net.pi.domain._

import scala.collection.JavaConverters._
import net.pi.matching.MatchersDefinitions._
import net.pi.matching.StrategiesDefinitions._
/**
  * Created by farid on 18/04/16.
  */
trait BrandingMatcherComponent {

  def brandingMatcherConfig :BrandingMatcherConfig

  trait BrandingMatcherConfig{
    def brandMatchers: List[List[(String=>Double)=>(Brand,Hotel)=>Option[MatchResult]]]
    def orgMatchers: List[List[(String=>Double)=>(Org,Hotel)=>Option[MatchResult]]]
    def strategy:(Stream[MatchResult] ,Hotel)=> AggregatedBrandingMatchResults
    def resultsSize: Int


  }

}

trait DefaultBrandingMatcherComponent extends  BrandingMatcherComponent{

  val brandingMatcherConfig = new DefaultBrandingMatcherConfig

  class DefaultBrandingMatcherConfig extends  BrandingMatcherConfig{

    private val matchingConfig = HotelBrandingConfig.config.getConfig("net.pi.hotel-branding.matching")

    private val brandMatchersConfig = matchingConfig.getConfig("matchers.brand")

    val brandBlocks = brandMatchersConfig.entrySet.asScala

    val brandMatchers:List[List[(String=>Double) =>(Brand, Hotel)=> Option[MatchResult]]] = for{
      block <- brandBlocks.toList
    }yield {
      for{
        matcher <- brandMatchersConfig.getStringList(block.getKey).asScala.toList
      }yield{
        brandMatchersDefinitionsMap(matcher).funct
      }
    }

    private val orgMatchersConfig = matchingConfig.getConfig("matchers.org")
    val orgBlocks = orgMatchersConfig.entrySet.asScala

    val orgMatchers:List[List[(String=>Double)=>(Org, Hotel) => Option[MatchResult]]] = for{
      block <- orgBlocks.toList
    }yield {
      for{
        matcher <- orgMatchersConfig.getStringList(block.getKey).asScala.toList
      }yield{
        orgMatchersDefinitionsMap(matcher).funct
      }
    }

    val strategy = strategiesDefinitionsMap(matchingConfig.getString("strategy")).strategy

    val resultsSize = matchingConfig.getInt("results-size")

  }

}