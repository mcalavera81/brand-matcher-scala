package net.pi.driver

import net.pi.matching.DefaultBrandingMatcherComponent
import net.pi.persistence.DefaultPersistenceComponent
import net.pi.service.DefaultHotelBrandingServiceComponent

/**
  * Created by farid on 18/04/16.
  */
object ApplicationLive {

  val hotelBrandingServiceComponent = new DefaultHotelBrandingServiceComponent with DefaultPersistenceComponent
                                      with DefaultBrandingMatcherComponent


  val brandingMatcherConfig = hotelBrandingServiceComponent.brandingMatcherConfig
  val hotelBrandingDAO = hotelBrandingServiceComponent.hotelBrandingDAO
  val hotelBrandingService = hotelBrandingServiceComponent.hotelBrandingService


}
