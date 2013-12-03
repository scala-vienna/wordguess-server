package actors

import scala.collection.mutable
import clashcode.logic.Player
import akka.actor.ActorRef
import org.joda.time.{ Seconds, DateTime }
import play.api.Logger

/**
 * statistics about each player are collected here
 * players are identified by ip address
 * players may change their names
 */
class ActorPlayer(var player: Player,
                  var actor: ActorRef,
                  var lastAction: DateTime = DateTime.now, // last message received from player
                  var totalGames: Int, // # total played games
                  var solvedGames: Int) // # total solved games
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

  /**
   * Let's manage one ActorPlayer instance for each player, even if she reconnects.
   *  If the player is renamed (new name, same IP) optionally a callback can be invoked.
   */
  def findActorPlayerCreatingIfNeeded(actor: ActorRef, playerName: String,
                                      onRename: (String, String) => Unit = (oldName, newName) => {}): ActorPlayer = {
    val optExisting = findActorPlayerByIP(actor)
    if (optExisting.isDefined) {
      Logger.debug(s"A player with this IP exists")
      val oldName = optExisting.get.player.name
      val newName = playerName
      Logger.debug(s"Replacing ${oldName} with ${newName}")
      onRename(oldName, newName)
    }
    val actorPlayer = optExisting getOrElse {
      // we don't know this ip address, lets create a new player
      val newActorPlayer = new ActorPlayer(
        Player(playerName),
        actor,
        lastAction = DateTime.now,
        totalGames = 0,
        solvedGames = 0)
      // register new ActorPlayer
      actorPlayers += newActorPlayer
      Logger.debug(s"Registered new actor ($playerName) with IP: ${newActorPlayer.ipAddress}")
      newActorPlayer
    }

    val uniquePlayerName = {
      val otherActorWithSameName = findDifferentPlayerAlreadyNamed(actorPlayer, playerName)
      if (otherActorWithSameName.isDefined) {
        // name exists, add IP for uniqueness
        val newName = playerName + "-" + actorPlayer.ipAddress
        Logger.debug(s"Changing name from $playerName to $newName")
        newName
      } else {
        // otherwise use name as-is (it's good enough)
        playerName
      }
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
    Logger.debug("Trying to find actor with IP: " + ipAddressToFind)
    val result = actorPlayers.find(actorPlayer => {
      Logger.debug(s"Comparing to ${actorPlayer.player.name} ${actorPlayer.ipAddress}")
      actorPlayer.ipAddress == ipAddressToFind
    })
    result.foreach(found => {
      Logger.debug(s"Found match: ${found.player.name}")
      found.actor = actor // update actor ref
    })
    result
  }

  def allPlayerActorsExcept(actorToExclude: ActorRef): Seq[ActorRef] = {
    (allPlayerActors) diff List(actorToExclude)
  }

  def allPlayerActors: Seq[ActorRef] = {
    actorPlayers.map(_.actor)
  }

  def actorPlayersOlderThan(maxSeconds: Int): Seq[ActorPlayer] = {
    actorPlayers.filter(_.isLastActionOlderThan(maxSeconds))
  }

}

object ActorPlayers {
  def getIpAddress(actor: ActorRef): String = {
    actor.path.address.host.getOrElse {
      Logger.warn(s"Returning 127.0.0.1 for actor: ${actor.toString}")
      "127.0.0.1"
    }
  }
}