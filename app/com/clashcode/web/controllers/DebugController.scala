package com.clashcode.web.controllers

import play.api.mvc._
import com.clashcode.web.views
import play.api.templates.Html
import clashcode.logic.GameWord

object DebugController extends Controller {

  var words = Seq[GameWord]()

  def dumpGameState = Action {
    val escapedText = (for (gameWord <- words) yield {
      val cssClass =
        if (gameWord.playing)
          "playing"
        else if (gameWord.solved)
          "solved"
        else ""
      s"""<span class="$cssClass">${gameWord.htmlStr}</span>"""
    }).toList.mkString

    Ok(views.html.gameState(Html(escapedText)))
  }

}