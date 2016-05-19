package net.pi.commons

import java.io.File

import com.github.tototoshi.csv.{CSVReader, CSVWriter, DefaultCSVFormat}
import net.pi.domain.{Brand, Hotel, MatchResult, Org}
import net.pi.matchingutils.MatchingConstants
import org.simmetrics.metrics.StringMetrics

import scala.io.BufferedSource

/**
  * Created by farid on 11/04/16.
  */
object Commons {

  val jaccard = StringMetrics.generalizedJaccard()
  val levenshtein = StringMetrics.levenshtein()

  val allowedChars = MatchingConstants.GOOD_CHARACTERS+"&"

  def using[X <: {def close(): Unit}, A](resource : X)(f : X => A) = {
    try {
      f(resource)
    } finally {
      resource.close()
    }
  }

  def getHostname(website: String):String={
    val validWebsite = if(website.startsWith("http")) website else s"http://$website"
    new java.net.URL(validWebsite).getHost.replaceAll("(www|com)", " ").split("\\.").maxBy (_.length)
  }


  def readWordFreq(fileName:String) ={
   /* implicit object MyFormat extends DefaultCSVFormat {
      override val delimiter = ' '
    }*/
    val input = io.Source.fromInputStream(getClass.getResourceAsStream("/"+fileName))
    val csvInput = CSVReader.open(input)
    csvInput.iterator.foldLeft[Map[String,Int]](Map.empty[String,Int]){
      (map, row) => map + (row.head->row(1).toInt)
    }
  }


  def writeMapToFile(fileName: String, wordFreqMap: Map[String,Int])={
      val mapToNestedList = wordFreqMap.toList.sortBy(a=>a._2)(Ordering.Int.reverse).map(_.productIterator.toList)
      using(CSVWriter.open(new File(fileName))){
          _.writeAll(mapToNestedList)
      }
  }

}
