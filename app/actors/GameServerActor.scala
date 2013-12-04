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
    with GameLogic with GameStatePersistence with ActorPlayers with GameParameters {

  override val gameState = initializeGameState()

  def initializeGameState(): GameState = {
    ensureGameStateFile(gameStateFilePath, "./source-text.txt", minGameWordLength)
    val gameState = loadFromFile(gameStateFilePath)
    restorePlayerState()
    gameState
  }

  case class HandleGuessNow(actorPlayer: ActorPlayer, letter: Char)

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName.take(16), sender) // name max 16 chars
    case MakeGuess(letter) => delayHandleGuess(sender, letter)
    case HandleGuessNow(actorPlayer, letter) => handleGuess(actorPlayer, letter)
    case SendToAll(msg) => broadCastToAll(msg)
    case ActorTick() => handleTick()
  }

  def restorePlayerState() {
    val f = new File("./player-state.txt")
    val src = Source.fromFile(f)
    val lines = src.getLines.filterNot(line => line.trim().isEmpty() || !line.contains('\t'))
    lines.foreach { line =>
      val parts = line.split('\t')
      if (parts.size == 7) {
        val ip = parts(0)
        val name = parts(1)
        val gamesSolved = parts(2).toInt
        val gamesTotal = parts(3).toInt
        val wordIdx = parts(4).toInt
        val letters = parts(5).map { c => if (c == '_') None else Some(c) }
        val triesLeft = parts(6).toInt

        val optGame =
          if (wordIdx >= 0) {
            val status = GameStatus(gameId = wordIdx, letters, triesLeft)
            Some(Game(wordIdx, status))
          } else {
            None
          }
        
        val player = Player(name)
        val actorPlayer = new ActorPlayer(player = player,
          actor = null,
          totalGames = gamesTotal,
          solvedGames = gamesSolved,
          givenIp = Some(ip))

        optGame foreach { game =>
          Logger.info(s"Restored game [id: ${wordIdx}] of player: ${name}")
          games += (player -> game) 
        }
        actorPlayers += actorPlayer
        Logger.info(s"Restored actorPlayer ${name} with IP: ${ip}")
      }
    }
  }

  def dumpPlayersState() {
    def actorPlayerDumpStr(actorPlayer: ActorPlayer) = {
      val player = actorPlayer.player
      val name = player.name
      val ip = actorPlayer.ipAddress
      val gamesSolved = actorPlayer.solvedGames
      val gamesTotal = actorPlayer.totalGames
      val (wordIdx, wordStatus, tries) = {
        getGame(player) map { game =>
          val gameWord = game.status.letters.map { _.getOrElse('_') }.mkString
          (game.wordIdx, gameWord, game.status.remainingTries)
        } getOrElse {
          (-1, "[none]", -1)
        }
      }
      Seq(ip, name, gamesSolved, gamesTotal, wordIdx, wordStatus, tries).mkString("\t")
    }
    val writer = new FileWriter("./player-state.txt")
    actorPlayers foreach { actorPlayer =>
      val str = actorPlayerDumpStr(actorPlayer)
      writer.write(str + "\n")
    }
    writer.close()
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    if (hasRemainingWords) {
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName, onRename = renameGamePlayerName)
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

  /** handle guess now or after 100 milliseconds */
  def delayHandleGuess(sender: ActorRef, letter: Char) {
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
      val actorHash = actorPlayer.actor.hashCode
      Logger.debug(s"${idx + 1}) $name: $ip - actorHash: $actorHash")
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
