package net.pi.config

/**
  * Created by farid on 11/04/16.
  */
object OutputEnum {
  sealed trait EnumVal
  case object Console extends EnumVal
  case object MongoDB extends EnumVal
}
