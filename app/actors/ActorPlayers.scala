package actors

import scala.collection.mutable
import clashcode.logic.Player
import akka.actor.ActorRef
import org.joda.time.{ Seconds, DateTime }

case class ActorPlayer(player: Player, actor: ActorRef, var lastAction: DateTime= DateTime.now()) {
  def isLastActionOlderThan(maxSeconds: Int): Boolean = {
    val now = DateTime.now()
    val secondsElapsed = Seconds.secondsBetween(lastAction, now).getSeconds()
    secondsElapsed > maxSeconds
  }
  def updateLastAction() {
    lastAction = DateTime.now()
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
      val newActorPlayer = ActorPlayer(Player(playerName), actor)
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

  def actorPlayersOlderThan(maxSeconds: Int): Seq[ActorPlayer] = {
    actorPlayers.filter(_.isLastActionOlderThan(maxSeconds))
  }

}