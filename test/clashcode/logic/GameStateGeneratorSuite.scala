package clashcode.logic

import org.scalatest.FunSuite

class GameStateGeneratorSuite extends FunSuite {

  test("generating a game-state") {
    val text = "This is a\ntext, with 2\nnewlines"
    val gameState = GameStateGenerator.fromText(text)
    assert(gameState.wordStates === 
      Seq(WordState("This", false), 
          WordState(" ", true),
          WordState("is", true),
          WordState(" ", true), 
          WordState("a", true), 
          WordState("\n", true), 
          WordState("text", false), 
          WordState(", ", true), 
          WordState("with", false), 
          WordState(" 2\n", true), 
          WordState("newlines", false)))
  }  
}