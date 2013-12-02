package com.clashcode.web.controllers

import clashcode.logic.GameWord
import actors.GameParameters
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue
import play.api.libs.json.Json

trait WordPushing { this:GameParameters => 

  protected def channel:Channel[JsValue]
  
  def pushWords(gameWords: Seq[GameWord]) = channel.push(
    Json.obj(
      "words" -> Json.toJson(gameWords.map(gameWordToMap(_)))
    )
  )

  private def gameWordToMap(gameWord: GameWord): Map[String, String] = {
    val cssClass =
      if (gameWord.playing)
        "playing"
      else if (gameWord.solved && gameWord.str.length() >= this.minGameWordLength)
        "solved"
      else ""

    Map("html" -> gameWord.htmlStr,
      "cssClass" -> cssClass
    )
  }  
  
}