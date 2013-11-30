package clashcode.logic

import clashcode.wordguess.messages.GameStatus
import scala.collection.mutable
import scala.util.Random

case class Player(name: String)
case class Game(wordIdx: Int, var status: GameStatus) {
  def isSolved = status.word.forall(_.isDefined)
}

trait GameLogic {

  val triesPerGame = 5

  def words: Seq[String]

  val games = mutable.Map[Player, Game]()

  def createGame(player: Player): Game = {
    val wordIdx = randomAvailableWordIndex
    def unsolvedWord(idx: Int) = words(wordIdx).map { ignoredChar => None }
    val gameStatus = GameStatus(word = unsolvedWord(idx = wordIdx), remainingTries = triesPerGame)
    val game = Game(wordIdx, status = gameStatus)
    games += player -> game
    game
  }

  def getGame(player: Player): Option[Game] = games.get(player)

  def onGameWon(player: Player, game: Game)
  def onGameLost(player: Player, game: Game)

  def makeGuess(player: Player, letter: Char) {
    getGame(player) map { game =>
      val wordToBeGuessed = words(game.wordIdx)
      if (wordToBeGuessed.contains(letter)) {
        val updatedStatusWord = for ((c, idx) <- wordToBeGuessed.zipWithIndex) yield {
          if (c == letter)
            Some(c)
          else
            game.status.word(idx)
        }
        game.status = game.status.copy(word = updatedStatusWord)
      } else {
        val decreasedTries = game.status.remainingTries - 1
        game.status = game.status.copy(remainingTries = decreasedTries)
      }
      checkIfWonOrLost(game, player)
    }
  }

  private def checkIfWonOrLost(game: Game, player: Player) {
    if (game.status.word.forall(_.isDefined)) {
      onGameWon(player, game)
    } else if (game.status.remainingTries == 0) {
      onGameLost(player, game)
    }
  }

  private def randomAvailableWordIndex: Int = {
    def availableWordIndexes: Set[Int] =
      ((0 until words.length).toSet -- takenWordIndexes)

    def takenWordIndexes: Set[Int] =
      games.values.map(_.wordIdx).toSet

    Random.shuffle(availableWordIndexes).head
  }

}

