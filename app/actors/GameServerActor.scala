package actors

import play.api.Logger
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import clashcode.wordguess.logic.Game
import clashcode.wordguess.logic.GameLogic
import clashcode.wordguess.logic.GameStatePersistence
import clashcode.wordguess.logic.Player
import clashcode.wordguess.logic.Token
import clashcode.wordguess.messages._
import com.clashcode.web.controllers.Application
import clashcode.wordguess.logic.GameState
import org.joda.time.DateTime
import java.io.FileWriter
import java.io.File
import scala.io.Source

trait GameParameters {
  // TODO: read all this from Play's config
  def timeOutSeconds = 5 * 60
  def gameStateFilePath = "./game-state.txt"
  def minGameWordLength = 3
  def maxRequestsPerSecond = 10 // prevent players from brute forcing
}

/**
 *
 */
class GameServerActor extends TickingActor
    with GameLogic with GameStatePersistence with ActorPlayers with GameParameters with PlayerStatePersistence {

  override val gameState = {
    val gameState = initializeGameState()
    restorePlayerState()
    gameState
  }

  def initializeGameState(): GameState = {
    ensureGameStateFile(gameStateFilePath, "./source-text.txt", minGameWordLength)
    loadFromFile(gameStateFilePath)
  }

  case class HandleGuessNow(actorPlayer: ActorPlayer, letter: Char)

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName.take(16), sender) // name max 16 chars
    case MakeGuess(letter) => delayHandleGuess(sender, letter)
    case HandleGuessNow(actorPlayer, letter) => handleGuess(actorPlayer, letter)
    case SendToAll(msg) => broadCastToAll(msg)
    case ActorTick() => handleTick()
  }

  def reassignActorRef(actor: ActorRef) {
    findActorPlayerByIP(actor) foreach { actorPlayer =>
      if (actorPlayer.actor == null) {
        Logger.debug("Re-assigned actorRef to: " + actorPlayer.player.name)
        actorPlayer.actor = actor
      }
    }
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    if (hasRemainingWords) {
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName, onRename = renameGamePlayerName)
      val player = actorPlayer.player
      val newOrExistingGame = getGame(player) getOrElse {
        createGame(player)
      }
      reassignActorRef(sender)
      val wordIdx = newOrExistingGame.wordIdx
      val word = words(newOrExistingGame.wordIdx)
      Logger.info(s"Player $playerName is playing for word: $word ($wordIdx)")
      sender ! newOrExistingGame.status
    } else {
      sender ! NoAvailableGames()
    }
  }

  private def newOrExistingGameFor(actorPlayer: ActorPlayer): Game = {
    val player = actorPlayer.player
    getGame(player) getOrElse {
      val newGame = createGame(player)
      Logger.info("Created game for player: " + player.name)
      newGame
    }
  }

  /** handle guess now or after 100 milliseconds */
  def delayHandleGuess(sender: ActorRef, letter: Char) {
    reassignActorRef(sender)
    debugActors()
    findActorPlayerByIP(actor = sender).foreach(actorPlayer => {
      val artificialDelay = (1000 / maxRequestsPerSecond - DateTime.now.getMillis + actorPlayer.lastAction.getMillis)
      if (artificialDelay > 0) {
        println("delaying answer by " + artificialDelay)
        runDelayed(artificialDelay) {
          self ! HandleGuessNow(actorPlayer, letter) // artificial delay to prevent brute force
        }
      } else {
        handleGuess(actorPlayer, letter) // handle it now
      }
    })
  }

  private def handleGuess(actorPlayer: ActorPlayer, letter: Char) {
    (for {
      game <- getGame(actorPlayer.player)
    } yield {
      makeGuess(actorPlayer.player, letter)
      if (!game.isOver) {
        Logger.info(s"""Player "${actorPlayer.player.name}" guessed '$letter'""")
        actorPlayer.updateLastAction
        actorPlayer.actor ! game.status
      }
    }) getOrElse {
      actorPlayer.actor ! NotPlayingError()
    }
  }

  private def debugActors() = {
    Logger.debug("We have these actors:")
    for ((actorPlayer, idx) <- actorPlayers.zipWithIndex) {
      val name = actorPlayer.player.name
      val ip = actorPlayer.ipAddress
      Logger.debug(s"${idx + 1}) $name: $ip")
    }
  }

  def broadCastToAll(msg: String) {
    allPlayerActors filter (_ != null) foreach { actor =>
      actor ! MsgToAll(msg)
    }
  }

  private def gameHash(game: Game): String = {
    s"g.${game.wordIdx}".hashCode().toHexString
  }

  override def onGameWon(player: Player, game: Game) {
    Logger.info(s"""Player "${player.name}" won a game""")
    sendGameOverMessage(player, msg = GameWon(finalStatus = game.status))
    persistGameState()
  }

  private def persistGameState() {
    writeToFile(gameState, gameStateFilePath)
  }

  override def onGameLost(player: Player, game: Game) {
    Logger.info(s"""Player "${player.name}" lost a game""")
    sendGameOverMessage(player, msg = GameLost(finalStatus = game.status))
  }

  private def sendGameOverMessage(player: Player, msg: GameOver) {
    findActorPlayer(player) map { actorPlayer =>

      // update game stats
      actorPlayer.totalGames += 1
      if (msg.isInstanceOf[GameWon]) actorPlayer.solvedGames += 1

      // send message
      val actor = actorPlayer.actor
      actor ! msg
    }
  }

  def handleTick() {

    // send updated player list to frontend
    Application.push(actorPlayers)
    // send updated word list to frontend
    Application.pushWords(gameWords)

    dumpPlayersState()

    purgeTimedOutGames()
  }

  private def purgeTimedOutGames() {
    actorPlayersOlderThan(timeOutSeconds) foreach { actorPlayer =>
      val player = actorPlayer.player
      if (getGame(player).isDefined) {
        removeGameOf(player)
        Logger.info("Removed timed-out game of: " + player.name)
      }
    }
  }

}
