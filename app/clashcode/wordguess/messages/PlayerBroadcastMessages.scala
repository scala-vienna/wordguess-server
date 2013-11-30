package clashcode.wordguess.messages

// =============
// Messages broadcast to all players

case class SuccessfulGuess(letter:Char, before: Seq[Option[Char]], after: Seq[Option[Char]])
