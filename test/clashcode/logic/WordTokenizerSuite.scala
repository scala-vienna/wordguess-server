package clashcode.logic

import org.scalatest.FunSuite

class WordTokenizerSuite extends FunSuite {

  test("should tokenize one word"){
    val tokens = WordTokenizer.tokenize("hello")
    assert(tokens === Seq(Word("hello")))
  }
  
  test("should tokenize two words an a space") {
    val tokens = WordTokenizer.tokenize("hello world")
    assert(tokens === Seq(Word("hello"), NonWord(" "), Word("world")))
  }
  
  test("should tokenize a series of non-word-chars together") {
    val tokens = WordTokenizer.tokenize("hello, world")
    assert(tokens === Seq(Word("hello"), NonWord(", "), Word("world")))
  }
  
}