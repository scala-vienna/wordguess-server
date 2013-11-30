package clashcode

import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

object ImplicitConversions {
  
  implicit class IntDuration(nr: Int) {
    def seconds = {
      FiniteDuration(nr, TimeUnit.SECONDS)
    }
    def second = seconds
  }  
  
}