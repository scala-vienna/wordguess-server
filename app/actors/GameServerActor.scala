package actors

import akka.actor._
import clashcode._
import com.clashcode.web.controllers.Application
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import org.joda.time.{ Seconds, DateTime }
import scala.collection.mutable
import play.api.libs.concurrent.Execution.Implicits._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.cluster.ClusterEvent._
import play.api.Logger
import akka.cluster.ClusterEvent.MemberRemoved
import scala.Some
import clashcode.Hello
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.ClusterEvent.MemberUp
import akka.actor.Identify
import akka.cluster.ClusterEvent.CurrentClusterState
import clashcode.PrisonerResponse
import clashcode.PrisonerRequest

import ImplicitConversions._

/**
 * 
 */
class GameServerActor extends Actor {

  context.system.scheduler.schedule(initialDelay = 1 second, interval = 1 second) {
    self ! TournamentTick()
  }

  def receive = {
    case RequestGame(playerId) => {
      
    }
    case MakeGuess(letter) => {
      
    }
    case _: TournamentTick => {
      
    }
  }
}
