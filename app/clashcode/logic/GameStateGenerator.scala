package clashcode.logic

import scala.collection.mutable

object GameStateGenerator {

  def fromText(text: String, minGameWordLength: Int = 4): GameState = {
    val tokens = WordTokenizer.tokenize(text)
    val wordStates = for ((token, idx) <- tokens.zipWithIndex) yield {
      val str = token.stringValue
      token match {
        case Word(str) => {
          if (str.length() >= minGameWordLength)
            WordState(str, solved = false)
          else
            WordState(str, solved = true)
        }
        case NonWord(str) => WordState(str, solved = true)
      }
    }
    GameState(wordStates.toList)
  }

}