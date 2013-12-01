package clashcode.logic

case class WordState(word: String, var solved: Boolean = false)
case class GameState(wordStates: List[WordState]) {
  val allWords = wordStates.map(_.word)
  def solvedWordIndexes =
    for {
      (wordState, idx) <- wordStates.zipWithIndex
      if (wordState.solved)
    } yield idx
  def markWordAsSolved(wordIdx: Int) {
    wordStates(wordIdx).solved = true
  }
}
