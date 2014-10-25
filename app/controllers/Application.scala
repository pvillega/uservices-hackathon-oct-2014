package controllers

import model.Message
import play.api.Logger
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Application extends Controller {

  val apiRoot = "http://combo-squirrel.herokuapp.com"
  val debugRoot = "http://private-anon-fb6d71fe1-combo.apiary-mock.com"
  val pathPOST = s"$apiRoot/topics/chat/facts"
  val user = "Pere"

  val msgForm = Form(
    mapping(
      "message" -> nonEmptyText
    )(Message.apply)(Message.unapply)
  )

  def index = Action { implicit request =>
    Logger.debug("Loading home page")
    Ok(views.html.index(msgForm))
  }

  // send message to the main server
  def sendMessage() = Action { implicit request =>
    msgForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.warn(s"Incorrect data when posting to submit message: $formWithErrors")
        BadRequest(views.html.index(formWithErrors))
      },
      msgData => {
        Logger.info(s"Message received > '${msgData.msg}'")
        val payload = Json.obj(
          "who" -> user,
          "says" -> msgData.msg
        )

        val holder: WSRequestHolder = WS.url(pathPOST)

        val futureResponse: Future[WSResponse] = holder.post(payload)
        futureResponse.map { wsResponse =>
          Logger.info(s"Response returned with status ${wsResponse.status} - ${wsResponse.body}")
        }

        Redirect(routes.Application.index)
      })
  }
}
