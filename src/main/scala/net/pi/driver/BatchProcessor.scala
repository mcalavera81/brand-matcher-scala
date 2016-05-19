package net.pi.driver

import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.scalalogging.LazyLogging
import net.pi.config.HotelBrandingConfig
import net.pi.config.HotelBrandingConfig.PersistenceConfig
import net.pi.domain.{Hotel, WriteResults}

import scala.collection.parallel.immutable.ParSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by farid on 4/04/16.
  */
object BatchProcessor extends LazyLogging{

  val (matches, nonMatches, totalCount) = (new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0))

  val appLive= ApplicationLive

  def main(args: Array[String]): Unit = {

    setUp()
    bookkeeping(findAndPersistBrandForAllHotels())

  }

  def setUp():Unit={
    logger.info("Dropping match-collection")
    appLive.hotelBrandingDAO.prepareOutput()
  }

  def findAndPersistBrandForAllHotels()={

    logger.info("Finding all hotels")
    var totalLength = 0
    var page = 0


    val hotelListFuture = appLive.hotelBrandingDAO.findHotels()

    for {
      hotelList <- hotelListFuture
    }yield {
      for {
        hotel <- hotelList.par
      } yield {
        val writeResults = appLive.hotelBrandingService.findBrandAndPersist(hotel)
        updateCounters(writeResults)
      }
    }

  }

  private def bookkeeping(results: Future[_]): Unit ={
    val startTime = System.currentTimeMillis()
    results.onComplete{_=>
      val endTime = System.currentTimeMillis()
      logger.info(s"Execution time : ${(endTime - startTime) / 1000}")
      logger.info(s"Finish hits: $matches")
      logger.info(s"Finish misses: $nonMatches")
      logger.info("End!")
      exportJson()
      System.exit(0)
    }

  }


  def exportJson()={

    import PersistenceConfig._

    val expCommand1 = s"mongoexport -h $servers --db $database --collection $hotel_collection_name --out /hotel_data/hotel_out.json --jsonArray"
    val expCommand2 = s"mongoexport -h $servers --db $database --collection $match_collection_name --out /hotel_data/matches.json --jsonArray"
    val export1 = Runtime.getRuntime.exec(expCommand1)
    val export2 = Runtime.getRuntime.exec(expCommand2)
    export1.waitFor()
    export2.waitFor()
  }

  private def updateCounters(writeResults: WriteResults): Unit ={
    val currentCount = totalCount.incrementAndGet()

    if (currentCount % 1000 == 0) logger.info(s"Count: $currentCount")

    if (writeResults.aggregatedMatchResults.maxScore == 0) {
      nonMatches.incrementAndGet()
    } else {
      matches.incrementAndGet()
    }
  }


}
