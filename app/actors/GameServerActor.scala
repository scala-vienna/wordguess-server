package actors

import clashcode.wordguess.messages._
import akka.actor.ActorRef
import clashcode.logic.Game
import clashcode.logic.GameLogic
import clashcode.logic.Player

/**
 *
 */
class GameServerActor extends TickingActor with GameLogic with ActorPlayers {

  // TODO: read from file or something like that
  override val words = Seq("hello", "world")

  def receive = {
    case RequestGame(playerName) => handleGameRequest(playerName, sender)
    case MakeGuess(letter) => handleGuess(sender, letter)
    case _: ActorTick => handleTick()
  }

  def handleGameRequest(playerName: String, sender: ActorRef) {
    removeExistingActorPlayerNamed(playerName)
    val actorPlayer = findActorPlayerCreatingIfNeeded(sender, playerName)
    val player = actorPlayer.player
    val game = getGame(player) getOrElse {
      createGame(player)
    }
    sender ! game.status
  }

  def handleGuess(sender: ActorRef, letter: Char) {
    findActorPlayer(actor = sender) map { actorPlayer =>
      val player = actorPlayer.player
      makeGuess(player, letter)
      getGame(player) map { game =>
        if (!game.isSolved)
          sender ! game.status
      }
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
