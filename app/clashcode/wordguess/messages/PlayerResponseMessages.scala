package clashcode.wordguess.messages

// =============
// Messages sent to particular game-player(s)

case class GameStarted(gameId: String)
case class GameStatus(word: Seq[Option[Char]], remainingTries: Int)

class GameOver
  case class GameWon(finalStatus: GameStatus) extends GameOver 
  case class GameLost(finalStatus: GameStatus) extends GameOver

// Sent to the player when there is no ongoing game with her/him.
case class NotPlayingError()

// Sent when there are no more available games. Client should quit.
case class NoAvailableGames()