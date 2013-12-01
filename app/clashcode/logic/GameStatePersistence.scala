package clashcode.logic

import scala.io.Source
import org.apache.commons.lang.StringEscapeUtils
import java.io.Writer
import java.io.File
import java.io.FileWriter

trait GameStatePersistence {

  val separatorChar = '§'
  
  def loadFrom(src: Source): GameState = {
    val wordStates = (for {
      line <- src.getLines
      if (!line.trim().isEmpty())
    } yield {
      val parts = line.split(separatorChar)
      if (parts.length == 2) {
        val solved = parts(0) == "S"
        // Scala src does one escaping for us
        val word = StringEscapeUtils.unescapeJava(
          StringEscapeUtils.unescapeJava(parts(1)))
        WordState(word, solved)
      } else {
        println("Problem with line: " + StringEscapeUtils.escapeJava(line))
        WordState("?", solved=true)
      }
    }).toList
    GameState(wordStates)
  }

  def write(state: GameState, writer: Writer) {
    for (wordState <- state.wordStates) {
      val stateLetter = if (wordState.solved) "S" else "U"
      val escapedWord = StringEscapeUtils.escapeJava(wordState.word)
      writer.write(s"$stateLetter$separatorChar$escapedWord\n")
    }
  }

  def ensureGameStateFile(gameStatePath: String, sourceTextPath: String) {
    val gameStateFile = new File(gameStatePath)
    val sourceTextFile = new File(sourceTextPath)
    assert(sourceTextFile.exists(), "File not found: " + sourceTextFile.getAbsolutePath())
    if (!gameStateFile.exists()) {
      val src = Source.fromFile(sourceTextFile)
      val gameState = GameStateGenerator.fromSource(src)
      writeToFile(gameState, gameStateFile)
    }
  }

  def loadFromFile(path: String): GameState =
    loadFromFile(new File(path))

  def writeToFile(state: GameState, path: String): Unit =
    writeToFile(state, new File(path))

  def loadFromFile(file: File): GameState = {
    val src = Source.fromFile(file)
    loadFrom(src)
  }

  def writeToFile(state: GameState, file: File) {
    val writer = new FileWriter(file)
    write(state, writer)
    writer.close()
  }

}