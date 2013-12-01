package actors

import play.api.Logger
import akka.actor.ActorRef

import clashcode.wordguess.messages._
import clashcode.logic._

/**
 *
 */
class GameServerActor extends TickingActor(intervalSecs = 5) with GameLogic with ActorPlayers {

  val timeOutSeconds = 5 * 60

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
    if (hasRemainingWords) {
      removeExistingActorPlayerNamed(playerName) // TODO: better remove on disconnect?
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName)
      val game = newOrExistingGameFor(actorPlayer)
      sender ! PlayingGame(gameId = gameHash(game))
      sender ! game.status
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
      actorPlayer <- findActorPlayer(actor = sender)
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
