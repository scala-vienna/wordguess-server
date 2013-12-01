package actors

import akka.actor.ActorRef

import clashcode.wordguess.messages._
import clashcode.logic._

/**
 *
 */
class GameServerActor extends TickingActor with GameLogic with ActorPlayers {

  // TODO: Initially solve uninteresting tokens (punctuation, new-lines, etc.)
  // TODO: Remove timed-out games?
  // TODO: prevent player from spawning more than one game?
  // TODO: use proper logging instead of println()
  

  // TODO: read from file or something like that
  override val words = Seq("hello", "world")

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName, sender)
    case MakeGuess(letter) => handleGuess(sender, letter)
    case _: ActorTick => handleTick()
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    if (hasRemainingWords) {
      removeExistingActorPlayerNamed(playerName)
      val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName)
      val player = actorPlayer.player
      val newOrExistingGame = getGame(player) getOrElse {
        createGame(player)
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
      if (!game.isSolved)
    } yield {
      sender ! game.status
      broadCastProgress(others = allPlayerActorsExcept(sender),
        letter,
        game.status.word)
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
    sendGameOverMessage(player, msg = GameWon(finalStatus = game.status))
  }

  override def onGameLost(player: Player, game: Game) {
    sendGameOverMessage(player, msg = GameLost(finalStatus = game.status))
  }

  private def sendGameOverMessage(player: Player, msg: GameOver) {
    findActorPlayer(player) map { actorPlayer =>
      val actor = actorPlayer.actor
      actor ! msg
    }
  }

  def handleTick() {
    def purgeTimedOutGames() {
      // TODO
    }
    purgeTimedOutGames()
  }

}
