package clashcode.wordguess.messages

// =============
// Messages sent to the server

case class RequestGame(playerName: String) // the name is descriptive, it is not an unique-id
case class MakeGuess(letter: Char)
