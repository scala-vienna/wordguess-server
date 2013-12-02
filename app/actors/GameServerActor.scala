package actors

import play.api.Logger
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import clashcode.logic.Game
import clashcode.logic.GameLogic
import clashcode.logic.GameStatePersistence
import clashcode.logic.Player
import clashcode.logic.Token
import clashcode.wordguess.messages._
import com.clashcode.web.controllers.Application
import clashcode.logic.GameState
import com.clashcode.web.controllers.DebugController

trait GameParameters {
  // TODO: read all this from Play's config
  def timeOutSeconds = 5 * 60
  def gameStateFilePath = "./game-state.txt"
  def minGameWordLength = 5
}

/**
 *
 */
class GameServerActor extends TickingActor
  with GameLogic with GameStatePersistence with ActorPlayers with GameParameters {

  override val gameState = initializeGameState()

  // partially solved text represented as tokens (for frontend)
  var tokens = Seq.empty[Token]

  def initializeGameState(): GameState = {
    ensureGameStateFile(gameStateFilePath, "./source-text.txt", minGameWordLength)
    loadFromFile(gameStateFilePath)
  }

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName, sender)
    case MakeGuess(letter) => handleGuess(sender, letter)
    case SendToAll(msg) => broadCastToAll(msg)
    case ActorTick() => handleTick()
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    if (hasRemainingWords) {
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName)
      actorPlayer.totalGames += 1 // update game stats
      val player = actorPlayer.player
      val newOrExistingGame = getGame(player) getOrElse {
        createGame(player)
      }
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

  def handleGuess(sender: ActorRef, letter: Char) {
    (for {
      actorPlayer <- findActorPlayerByIP(actor = sender)
      player = actorPlayer.player
      game <- getGame(player)
    } yield {
      makeGuess(player, letter)
      if (!game.isOver) {
        Logger.info(s"""Player "${player.name}" guessed '$letter'""")
        actorPlayer.updateLastAction
        sender ! game.status
      }
    }) getOrElse {
      sender ! NotPlayingError()
    }
  }

  def broadCastToAll(msg: String) {
    allPlayerActors foreach { actor =>
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
      val actor = actorPlayer.actor
      actor ! msg
    }
  }

  def handleTick() {

    Application.push(actorPlayers) // send updated player list to frontend
    Application.pushTokens(tokens) // send updated token list to frontend

    DebugController.pushWords(gameWords)

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
