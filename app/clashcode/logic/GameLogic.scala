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

  val solvedWordIndexes = mutable.Buffer[Int]()

  val games = mutable.Map[Player, Game]()

  def hasRemainingWords = availableWordIndexes.size > 0

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
      val wordContainsLetter = wordToBeGuessed.toLowerCase().contains(letter.toLower)
      if (wordContainsLetter) {
        val updatedStatusWord = for ((c, idx) <- wordToBeGuessed.zipWithIndex) yield {
          if (c.toLower == letter.toLower)
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
      addSolvedWordIndex(game)
      removeGameOf(player)
      onGameWon(player, game)
    } else if (game.status.remainingTries == 0) {
      removeGameOf(player)
      onGameLost(player, game)
    }
  }
  
  private def removeGameOf(player:Player) {
    games.remove(player)
  }

  private def addSolvedWordIndex(game: Game) {
    solvedWordIndexes += game.wordIdx
  }

  private def availableWordIndexes: Set[Int] = {
    val inGameWordIndexes = games.values.map(_.wordIdx)
    val unavailableWordIndexes: Set[Int] =
      (inGameWordIndexes ++ solvedWordIndexes).toSet
    ((0 until words.length).toSet -- unavailableWordIndexes)
  }

  private def randomAvailableWordIndex: Int = {
    Random.shuffle(availableWordIndexes).head
  }

}

