package twitter.login

import diode._
import org.scalajs.dom
import twitter.login.LoginRest.SuccessfulLogin
import twitter.tweets.{ClearCredentials, LoginSuccessful}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

case class LoginModel(username: String, password: String, progress: Boolean, error: Option[String])

object LoginModel {
  val initModel = LoginModel("", "", progress = false, None)
}

case class UsernameChanged(username: String) extends Action
case class PasswordChanged(username: String) extends Action
case object LoginSubmit extends Action
case class LoginError(error: String) extends Action
case object Logout extends Action
case object TryInitialLogin extends Action

class LoginHandler[M](modelRW: ModelRW[M, LoginModel]) extends ActionHandler(modelRW) {
  private val credentialsKey = "credentials"

  private def storeCredentials(credentials: String): Unit =
    dom.window.localStorage.setItem(credentialsKey, credentials)

  private def loadCredentials(): Option[String] = {
    val credentials = dom.window.localStorage.getItem(credentialsKey)
    if (credentials == null || credentials == "null") None else Some(credentials)
  }

  override def handle = {
    case UsernameChanged(u) =>
      updated(value.copy(username = u))
    case PasswordChanged(p) =>
      updated(value.copy(password = p))
    case LoginError(e) =>
      updated(value.copy(progress = false, error = Some(e)))
    case LoginSubmit =>
      val effect = Effect(LoginRest.tryLogin(value.username, value.password)
        .map {
          case Left(e) => LoginError(e.getMessage)
          case Right(SuccessfulLogin(token)) =>
            val credentials = s"Bearer $token"
            storeCredentials(credentials)
            LoginSuccessful(credentials)
        })
      updated(value.copy(progress = true), effect)
    case Logout =>
      storeCredentials(null)
      updated(LoginModel.initModel, Effect(Future.successful(ClearCredentials)))
    case TryInitialLogin =>
      loadCredentials() match {
        case Some(credentials) =>
          val effect = Effect(LoginRest.tryRefreshToken(credentials)
            .map {
              case Left(_) => NoAction
              case Right(SuccessfulLogin(token)) =>
                val credentials = s"Bearer $token"
                storeCredentials(credentials)
                LoginSuccessful(credentials)
            })
          updated(value, effect)
        case None => noChange
      }
  }
}
