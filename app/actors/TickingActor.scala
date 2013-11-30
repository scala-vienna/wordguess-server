package actors

import akka.actor._
import play.api.libs.concurrent.Execution.Implicits._

import clashcode.ImplicitConversions._
import clashcode.messages._

case class ActorTick()

/**
 * Sends ticks to itself at regular intervals
 * 
 * The implementation of receive should handle the ticking message
 * 
 */
abstract class TickingActor(initialDelaySecs: Int = 1, intervalSecs: Int = 1) extends Actor {

  context.system.scheduler.schedule(
    initialDelay = initialDelaySecs seconds,
    interval = intervalSecs seconds) {
      self ! ActorTick()
    }

}