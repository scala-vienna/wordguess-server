package clashcode.messages

// =============
// Messages sent to the server

case class RequestGame(playerId: String)
case class MakeGuess(letter: Char)
