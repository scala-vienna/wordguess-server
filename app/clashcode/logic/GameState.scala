package clashcode.logic


case class WordState(word:String, solved:Boolean)
case class GameState(wordStates:List[WordState])
