package net.pi.domain

/**
  * Created by farid on 25/04/16.
  */
case class BrandingInitialization(
                                   processedBrands:Set[Brand],
                                   processedOrgs:Set[Org],
                                   allStopWords:Set[String],
                                   idf: String => Double)
