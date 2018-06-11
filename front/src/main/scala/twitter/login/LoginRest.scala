package twitter.login

import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.ext.{Ajax, AjaxException}
import twitter.MainApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LoginRest {

  case class MessageInfo(message: String)

  case class LoginInfo(username: String, password: String)

  case class SuccessfulLogin(token: String)

  def authorizationHeader(credentials: String): Map[String, String] =
    Map("Authorization" -> credentials)

  implicit class AjaxFuture[A](val value: Future[Either[Exception, A]]) extends AnyVal {
    def recoverAjax(): Future[Either[Exception, A]] = value.recover {
      case AjaxException(xhr) if xhr.status == 401 => Left(new Exception("Nieprawidłowa nazwa użytkownika lub hasło."))
      case AjaxException(xhr) if xhr.status == 406 => Left(new Exception("Przekroczono dozwoloną liczbę obiektów tego typu."))
      case AjaxException(_) => Left(new Exception(s"Błąd połączenia"))
      case e: Exception => Left(e)
    }
  }

  def tryLogin(username: String, password: String): Future[Either[Exception, SuccessfulLogin]] = {
    val url = s"${MainApp.apiUrl}/login"
    val json = LoginInfo(username, password).asJson.noSpaces
    Ajax.post(url, data = InputData.str2ajax(json))
      .map(r => decode[SuccessfulLogin](r.responseText))
      .recoverAjax()
  }

  def tryRefreshToken(credentials: String): Future[Either[Exception, SuccessfulLogin]] = {
    val url = s"${MainApp.apiUrl}/refresh_token"
    Ajax.get(url, headers = authorizationHeader(credentials))
      .map(r => decode[SuccessfulLogin](r.responseText))
      .recoverAjax()
  }
}
