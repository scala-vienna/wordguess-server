package actors

import play.api.Logger
import akka.actor.ActorRef

import clashcode.wordguess.messages._
import clashcode.logic._

/**
 *
 */
class GameServerActor extends TickingActor(intervalSecs = 5) with GameLogic with ActorPlayers {

  val timeOutSeconds = 5*60

  // TODO: read from file or something like that
  // TODO: make global progress persistent
  override val words = Seq("hello", "world")
  
  // TODO: Initially solve uninteresting tokens (punctuation, new-lines, etc.)
  // TODO: prevent player from spawning more than one game?  

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName, sender)
    case MakeGuess(letter) => handleGuess(sender, letter)
    case ActorTick() => handleTick()
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    Logger.info("Game request for player: " + playerName)
    if (hasRemainingWords) {
      removeExistingActorPlayerNamed(playerName)
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName)
      val player = actorPlayer.player
      val newOrExistingGame = getGame(player) getOrElse {
        val newGame = createGame(player)
        Logger.info("Created game for player: " + playerName)
        newGame
      }
      sender ! newOrExistingGame.status
    } else {
      sender ! NoAvailableGames()
    }
  }

  def handleGuess(sender: ActorRef, letter: Char) {
    (for {
      actorPlayer <- findActorPlayer(actor = sender)
      player = actorPlayer.player
      _ = makeGuess(player, letter)
      game <- getGame(player)
    } yield {
      if (!game.isSolved) {
        Logger.info(s"""Player "${player.name}" guessed '$letter'""")
        actorPlayer.updateLastAction
        sender ! game.status
        broadCastProgress(others = allPlayerActorsExcept(sender),
          letter,
          game.status.word)
      }
    }) getOrElse {
      sender ! NotPlayingError()
    }
  }

  private def broadCastProgress(others: Seq[ActorRef], letter: Char, word: Seq[Option[Char]]) {
    others foreach { otherPlayerActor =>
      otherPlayerActor ! SuccessfulGuess(letter, word)
    }
  }

  override def onGameWon(player: Player, game: Game) {
    Logger.info(s"""Player "${player.name}" won a game""")
    sendGameOverMessage(player, msg = GameWon(finalStatus = game.status))
  }

  override def onGameLost(player: Player, game: Game) {
    Logger.info(s"""Player "${player.name}" lost a game""")
    sendGameOverMessage(player, msg = GameLost(finalStatus = game.status))
  }

  private def sendGameOverMessage(player: Player, msg: GameOver) {
    findActorPlayer(player) map { actorPlayer =>
      val actor = actorPlayer.actor
      removeExistingActorPlayerNamed(player.name)
      actor ! msg
    }
  }

  def handleTick() {
    purgeTimedOutGames()
  }

  private def purgeTimedOutGames() {
    actorPlayersOlderThan(timeOutSeconds) foreach { actorPlayer =>
      val player = actorPlayer.player
      removeGameOf(player)
      removeExistingActorPlayerNamed(player.name)
      Logger.info("Removed timed-out game of: " + player.name)
    }
  }

}
