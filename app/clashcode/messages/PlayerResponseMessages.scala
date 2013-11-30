package clashcode.messages

// =============
// Messages sent to particular game-player(s)

case class GameStarted(initialStatus: GameStatus)
case class GameStatus(word: Seq[Option[Char]], remainingTries: Int)

case class GameWon(finalStatus: GameStatus) 
case class GameLost(finalStatus: GameStatus) 

// sent to the player when there is no ongoing game with it
case class NoOngoingGameError()  
