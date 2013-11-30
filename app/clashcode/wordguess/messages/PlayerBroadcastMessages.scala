package clashcode.wordguess.messages

// =============
// Messages broadcast to all players

case class SuccessfulGuess(letter:Char, word: Seq[Option[Char]])
