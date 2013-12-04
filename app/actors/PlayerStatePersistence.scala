package actors

import scala.io.Source
import java.io.File
import java.io.FileWriter

import play.api.Logger

import clashcode.wordguess.messages.GameStatus
import clashcode.logic.Game
import clashcode.logic.Player
import clashcode.logic.GameLogic

trait PlayerStatePersistence { this: GameLogic with ActorPlayers =>

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

}