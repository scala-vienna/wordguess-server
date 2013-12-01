package actors

import scala.collection.mutable
import clashcode.logic.Player
import akka.actor.ActorRef
import org.joda.time.{ Seconds, DateTime }

case class ActorPlayer(player: Player, actor: ActorRef, lastAction: DateTime) {
  def isLastActionOlderThan(seconds: Int): Boolean = {
    val now = DateTime.now()
    Seconds.secondsBetween(now, lastAction).getSeconds() > seconds
  }
}

trait ActorPlayers {
  val actorPlayers = mutable.Buffer[ActorPlayer]()

  def removeExistingActorPlayerNamed(playerName: String) {
    findActorPlayer(playerName) map { actorPlayer =>
      actorPlayers -= actorPlayer
    }
  }

  def findActorPlayerCreatingIfNeeded(actor: ActorRef, playerName: String): ActorPlayer = {
    val optExisting = findActorPlayer(actor)
    optExisting getOrElse {
      val now = DateTime.now()
      val newActorPlayer = ActorPlayer(Player(playerName), actor, lastAction = now)
      actorPlayers += newActorPlayer
      newActorPlayer
    }
  }

  def findActorPlayer(playerName: String): Option[ActorPlayer] = {
    actorPlayers.find(actorPlayer => actorPlayer.player.name == playerName)
  }

  def findActorPlayer(player: Player): Option[ActorPlayer] = {
    actorPlayers.find(actorPlayer => actorPlayer.player == player)
  }

  def findActorPlayer(actor: ActorRef): Option[ActorPlayer] = {
    actorPlayers.find(actorPlayer => actorPlayer.actor == actor)
  }

  def allPlayerActorsExcept(actorToExclude: ActorRef): Seq[ActorRef] = {
    actorPlayers.map(_.actor) - actorToExclude
  }

  def findWhereLastActionOlderThan(seconds: Int): Seq[ActorPlayer] = {
    actorPlayers.filter(_.isLastActionOlderThan(seconds))
  }

}