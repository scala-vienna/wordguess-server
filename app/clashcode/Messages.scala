package clashcode

// ==================
// Messages sent to server

case class RequestGame(playerId: String)
case class MakeGuess(letter: Char)

// ==================
// Messages sent to client

// Sent to the player participating in a game
case class GameStarted(initialStatus: GameStatus)
case class GameStatus(word: Seq[Option[Char]], remainingTries: Int)

case class GameWon(finalStatus: GameStatus) 
case class GameLost(finalStatus: GameStatus) 

case class NoOngoingGameError()  // sent to the player when there is no ongoing game with it

// Broadcast to all players
case class SuccessfulGuess(letter:Char, before: Seq[Option[Char]], after: Seq[Option[Char]])

// ==================

/** [Investigator] Ask prisoners name */
case object NameRequest

/** [Prisoner] Tell investigator your name (max 12 chars) */
case class Hello(name: String)

/** [Investigator] Tells the name of other prisoner, asks: do you cooperate or defect? */
case class PrisonerRequest(name: String)

/** [Prisoner] Answer to PrisonerRequest: cooperate or defect. */
case class PrisonerResponse(cooperate: Boolean)

/** [Investigator] Tells the prisoner whether the other one cooperated */
case class PrisonerResult(name: String, cooperate: Boolean)
