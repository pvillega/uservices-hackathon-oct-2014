package controllers

import java.util.UUID

import actors.UserActor
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

object Application extends Controller {

  val UID = "uid"

  def index = Action { implicit request =>
    val uid = request.session.get(UID).getOrElse(UUID.randomUUID().toString)
    Ok(views.html.index(uid)).withSession {
      Logger.debug("Creation uid " + uid)
      request.session + (UID -> uid)
    }
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful(request.session.get(UID) match {
      case None => Left(Forbidden)
      case Some(uid) => Right(UserActor.props(uid))
    })
  }
}
