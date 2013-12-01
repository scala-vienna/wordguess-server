package clashcode.logic

import scala.io.Source
import org.apache.commons.lang.StringEscapeUtils
import java.io.Writer

trait GameStatePersistence {

  def loadFrom(src: Source): GameState = {
    val wordStates = (for {
      line <- src.getLines
      if (!line.trim().isEmpty())
    } yield {
      val parts = line.split(":")
      val solved = parts(0) == "S"
      // Scala src does one escaping for us
      val word = StringEscapeUtils.unescapeJava(
        StringEscapeUtils.unescapeJava(parts(1)))
      WordState(word, solved)
    }).toList
    GameState(wordStates)
  }

  def write(state: GameState, writer: Writer) {
    for(wordState <- state.wordStates) {
      val stateLetter = if(wordState.solved) "S" else "U"
      val escapedWord = StringEscapeUtils.escapeJava(wordState.word)
      println(s"writing word: $escapedWord")
      writer.write(s"$stateLetter:$escapedWord\n")
    }
  }

}