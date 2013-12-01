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

/**
 *
 */
class GameServerActor extends TickingActor
  with GameLogic with GameStatePersistence with ActorPlayers {

  val timeOutSeconds = 5 * 60
  val gameStateFilePath = "./game-state.txt"

  override val gameState = initializeGameState()

  // partially solved text represented as tokens (for frontend)
  var tokens = Seq.empty[Token]

  def initializeGameState(): GameState = {
    ensureGameStateFile(gameStateFilePath, "./source-text.txt", minGameWordLength = 5)
    loadFromFile(gameStateFilePath)
  }

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName, sender)
    case MakeGuess(letter) => handleGuess(sender, letter)
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
      sender ! PlayingGame(gameId = gameHash(newOrExistingGame))
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
      if (!game.isSolved) {
        Logger.info(s"""Player "${player.name}" guessed '$letter'""")
        actorPlayer.updateLastAction
        sender ! game.status
        broadCastProgress(recipients = allPlayerActorsExcept(sender),
          letter,
          game)
      }
    }) getOrElse {
      sender ! NotPlayingError()
    }
  }

  private def broadCastProgress(recipients: Seq[ActorRef], letter: Char, game: Game) {
    val gameId = gameHash(game)
    val word = game.status.word
    recipients foreach { actor =>
      actor ! SuccessfulGuess(gameId, letter, word)
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

    DebugController.words = gameWords
    
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
