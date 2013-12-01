package actors

import scala.collection.mutable
import clashcode.logic.Player
import akka.actor.ActorRef
import org.joda.time.{ Seconds, DateTime }

/**
 * statistics about each player are collected here
 * players are identified by ip address
 * players may change their names
 */
class ActorPlayer(var player: Player,
  var actor: ActorRef,
  var lastAction: DateTime = DateTime.now, // last message received from player
  var totalGames: Int) // # total played games
  {
  val ipAddress = ActorPlayers.getIpAddress(actor)

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

  /** let's manage one ActorPlayer instance for each player, even if she reconnects */
  def findActorPlayerCreatingIfNeeded(actor: ActorRef, playerName: String): ActorPlayer = {
    val optExisting = findActorPlayerByIP(actor)
    val actorPlayer = optExisting getOrElse {
      // we don't know this ip address, lets create a new player
      val newActorPlayer = new ActorPlayer(
        Player(playerName),
        actor,
        lastAction = DateTime.now,
        totalGames = 0)
      // register new ActorPlayer
      actorPlayers += newActorPlayer
      newActorPlayer
    }

    val uniquePlayerName = {
      val otherActorWithSameName = findDifferentPlayerAlreadyNamed(actorPlayer, playerName)
      if (otherActorWithSameName.isDefined)
        // name exists, add IP for uniqueness
        playerName + "-" + actorPlayer.ipAddress
      else
        // otherwise use name as-is (it's good enough)
        playerName
    }

    // update our records about this player
    actorPlayer.player = Player(uniquePlayerName) // player may have changed her name
    actorPlayer.lastAction = DateTime.now
    actorPlayer.actor = actor // maybe it's a new actor
    actorPlayer
  }

  private def findDifferentPlayerAlreadyNamed(playerToCompare: ActorPlayer, playerName: String) = {
    actorPlayers.find(p => p != playerToCompare && p.player.name == playerName)
  }

  def findActorPlayer(playerName: String): Option[ActorPlayer] = {
    actorPlayers.find(actorPlayer => actorPlayer.player.name == playerName)
  }

  def findActorPlayer(player: Player): Option[ActorPlayer] = {
    actorPlayers.find(actorPlayer => actorPlayer.player == player)
  }

  def findActorPlayerByIP(actor: ActorRef): Option[ActorPlayer] = {
    val ipAddressToFind = ActorPlayers.getIpAddress(actor)
    actorPlayers.find(actorPlayer => actorPlayer.ipAddress == ipAddressToFind)
  }

  def allPlayerActorsExcept(actorToExclude: ActorRef): Seq[ActorRef] = {
    actorPlayers.map(_.actor) - actorToExclude
  }

  def actorPlayersOlderThan(maxSeconds: Int): Seq[ActorPlayer] = {
    actorPlayers.filter(_.isLastActionOlderThan(maxSeconds))
  }

}

object ActorPlayers {
  def getIpAddress(actor: ActorRef): String = {
    actor.path.address.host.getOrElse("127.0.0.1")
  }
}