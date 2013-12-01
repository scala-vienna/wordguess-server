package clashcode.wordguess.messages

// =============
// Messages broadcast to all players

case class SuccessfulGuess(
  gameId: String,
  letter: Char,
  word: Seq[Option[Char]])
