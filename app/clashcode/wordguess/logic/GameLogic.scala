package clashcode.wordguess.logic

import clashcode.wordguess.messages.GameStatus
import scala.collection.mutable
import scala.util.Random

case class Player(name: String)
case class Game(wordIdx: Int, var status: GameStatus) {
  def isSolved = status.letters.forall(_.isDefined)
  def isOver = isSolved || status.remainingTries <= 0
  def displayWord = status.letters.map(c => c.getOrElse('_')).mkString
}

case class GameWord(idx: Int, str: String, playing: Boolean, solved: Boolean) {
  def htmlStr = str.replaceAll("\n", "<br/>")
}

trait GameLogic {

  val triesPerGame = 5

  def gameState: GameState

  final def words: Seq[String] = gameState.allWords
  final def solvedWordIndexes = gameState.solvedWordIndexes

  val games = mutable.Map[Player, Game]()

  def hasRemainingWords = availableWordIndexes.size > 0

  def createGame(player: Player): Game = {
    val wordIdx = randomAvailableWordIndex
    def unsolvedWord(idx: Int) = words(wordIdx).map { ignoredChar => None }
    val gameStatus = GameStatus(wordIdx, letters = unsolvedWord(idx = wordIdx), remainingTries = triesPerGame)
    val game = Game(wordIdx, status = gameStatus)
    games += (player -> game)
    game
  }

  def getGame(player: Player): Option[Game] = games.get(player)

  def onGameWon(player: Player, game: Game)
  def onGameLost(player: Player, game: Game)
  
  def gameWords: Seq[GameWord] = {
    val solvedIdxList = gameState.solvedWordIndexes
    for ((wordState, idx) <- gameState.wordStates.zipWithIndex) yield {
        val solvedWord = gameState.allWords(idx)
      val solved = solvedIdxList.contains(idx)
      if (solved) {
        GameWord(idx, solvedWord, playing = false, solved = true)
      } else {
        val optGame = games.values.find(game => game.wordIdx == idx)
        val playing = optGame.isDefined
        val hiddenWord = solvedWord.map(_ => '_')
        val word = optGame map (game => game.displayWord) getOrElse (hiddenWord)
        GameWord(idx, word, playing, solved = false)
      }
    }
  }

  def makeGuess(player: Player, letter: Char) {
    getGame(player) map { game =>
      val wordToBeGuessed = words(game.wordIdx)
      val wordContainsLetter = wordToBeGuessed.toLowerCase().contains(letter.toLower)
      if (wordContainsLetter) {
        val updatedStatusWord = for ((c, idx) <- wordToBeGuessed.zipWithIndex) yield {
          if (c.toLower == letter.toLower)
            Some(c)
          else
            game.status.letters(idx)
        }
        game.status = game.status.copy(letters = updatedStatusWord)
      } else {
        val decreasedTries = game.status.remainingTries - 1
        game.status = game.status.copy(remainingTries = decreasedTries)
      }
      checkIfWonOrLost(game, player)
    }
  }

  private def checkIfWonOrLost(game: Game, player: Player) {
    if (game.status.letters.forall(_.isDefined)) {
      addSolvedWordIndex(game)
      removeGameOf(player)
      onGameWon(player, game)
    } else if (game.status.remainingTries == 0) {
      removeGameOf(player)
      onGameLost(player, game)
    }
  }
  
  def renameGamePlayerName(oldPlayerName: String, newPlayerName: String) {
    val oldPlayer = Player(oldPlayerName)
    getGame(oldPlayer) foreach { game =>
      removeGameOf(oldPlayer)
      games += (Player(newPlayerName) -> game)
    }
  }

  def removeGameOf(player: Player) {
    games.remove(player)
  }

  private def addSolvedWordIndex(game: Game) {
    gameState.markWordAsSolved(game.wordIdx)
  }

  private def inGameWordIndexes = games.values.map(_.wordIdx)

  private def availableWordIndexes: List[Int] = {
    val unavailableWordIndexes: Set[Int] =
      (inGameWordIndexes ++ solvedWordIndexes).toSet
    ((0 until words.length).toSet -- unavailableWordIndexes).toList
  }

  private def randomAvailableWordIndex: Int = {
    val randomIndexes = Random.shuffle(availableWordIndexes)
    randomIndexes.head
  }

}

