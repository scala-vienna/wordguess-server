package clashcode.logic

import scala.collection.mutable
import scala.io.Source

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

  def fromSource(src: Source, minGameWordLength: Int = 4): GameState = {
    val wholeStr = src.getLines.mkString("\n")
    fromText(wholeStr, minGameWordLength)
  }

}