package com.clashcode.web.controllers

import play.api.mvc._
import com.clashcode.web.views
import play.api.templates.Html

object DebugController extends Controller {

  var words = Seq[String]()
  
  def dumpGameState = Action {
    val escapedText = (for(word <- words) yield {
      word.replaceAll("\n", "<br/>")
    }).toList.mkString
    
    Ok(views.html.gameState(Html(escapedText)))
  }
  
}