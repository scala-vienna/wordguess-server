package com.clashcode.web.controllers

import play.api.mvc._
import play.api.libs.iteratee.{ Iteratee, Concurrent }
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import com.clashcode.web.views
import play.api.templates.Html
import clashcode.logic.GameWord
import actors.GameParameters
import play.api.libs.iteratee.Concurrent.Channel

object DebugController extends Controller with GameParameters with WordPushing {

  protected val (out, channel) = Concurrent.broadcast[JsValue]

  def dumpGameState = Action { implicit request =>
    val webSocketUrl = routes.DebugController.liveFeed().webSocketURL()
    Ok(views.html.gameState(webSocketUrl))
  }

  def liveFeed = WebSocket.using[JsValue] { request =>
    Logger.info("new listener")
    // ignore incoming websocket traffic
    val in = Iteratee.foreach[JsValue] {
      msg =>
        Logger.debug(msg.toString)
        val action = (msg \ "action").asOpt[String].getOrElse("")
    } map {
      _ => Logger.info("removed listener")
    }
    (in, out)
  }

}