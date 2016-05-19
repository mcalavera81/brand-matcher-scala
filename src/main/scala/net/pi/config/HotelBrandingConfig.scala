package net.pi.config

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}


/**
  * Created by farid on 12/04/16.
  */
object HotelBrandingConfig {

  val config = ConfigUtil.loadFromEnvironment()

  object PersistenceConfig {

    private val persistenceConfig = config.getConfig("net.pi.hotel-branding.persistence")

    val servers= persistenceConfig.getString("mongodb.servers")
    val database = persistenceConfig.getString("mongodb.database")
    val hotel_collection_name = persistenceConfig.getString("mongodb.hotel-collection")
    val brand_collection_name = persistenceConfig.getString("mongodb.brand-collection")
    val match_collection_name = persistenceConfig.getString("mongodb.match-collection")

    val UPDATE_HOTEL_MODEL = persistenceConfig.getBoolean("update-hotel-model")
    val BATCH_SIZE = persistenceConfig.getInt("batch-size")

  }

  object MatchingConfig {
    private val matchingConfig = config.getConfig("net.pi.hotel-branding.matching")


    val FILTER_CHARS = matchingConfig.getBoolean("filter-characters")
    val JACCARD_THRESHOLD = matchingConfig.getDouble("jaccard-threshold")
    val LEVENSHTEIN_THRESHOLD = matchingConfig.getDouble("levenshtein-threshold")
    val DOC_COUNT = matchingConfig.getInt("doc-count")

  }


}

object ConfigUtil{

  def loadFromEnvironment():Config={
    Option(System.getProperty("config.file")).fold(ConfigFactory.load(System.getProperty(
      "config.resource", "application.conf")))(f => ConfigFactory.parseFile(new File(f)).resolve())

  }
}
