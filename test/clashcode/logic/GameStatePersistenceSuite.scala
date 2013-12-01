package clashcode.logic

import org.scalatest.FunSuite
import scala.io.Source
import java.io.StringWriter

class GameStatePersistenceSuite extends FunSuite {

  test("load game state") {
    new GameStatePersistence {
      val src = Source.fromString("""
          |U§This
          |S§is
          |S§a
          |U§text""".stripMargin)
      val gameState = loadFrom(src)
      assert(gameState === GameState(List(
        WordState("This", false),
        WordState("is", true),
        WordState("a", true),
        WordState("text", false))))
    }
  }

  test("handle escaped values when loading") {
    new GameStatePersistence {
      val src = Source.fromString(raw"S§\n\t")
      val gameState = loadFrom(src)
      assert(gameState === GameState(List(
        WordState("\n\t", true))))
    }
  }

  test("write state, escaping as needed") {
    new GameStatePersistence {
      val state = GameState(List(
        WordState("This", false),
        WordState("is", true),
        WordState("\n", true),
        WordState("a", true),
        WordState("text", false)))
      val writer = new StringWriter()
      write(state, writer)
      assert(writer.toString() === """U§This
          |S§is
          |S§\n
          |S§a
          |U§text
          |""".stripMargin)
    }
  }

}