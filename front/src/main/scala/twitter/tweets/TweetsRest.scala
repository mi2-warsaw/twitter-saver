package twitter.tweets

import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData
import twitter.MainApp
import twitter.login.LoginRest.{AjaxFuture, MessageInfo, authorizationHeader}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Date

object TweetsRest {

  case class TweetListItem(id: Int, source: String, objectType: String, query: String, deleted: Boolean)

  def tweetsList(credentials: String): Future[Seq[TweetListItem]] = {
    val url = s"${MainApp.apiUrl}/objects"
    val headers = authorizationHeader(credentials)
    Ajax.get(url, headers = headers)
      .map(r => decode[Seq[TweetListItem]](r.responseText))
      .recoverAjax()
      .flatMap {
        case Right(xs) => Future.successful(xs)
        case Left(e) => Future.failed(e)
      }
  }

  sealed trait TweetSource
  case object StreamSource extends TweetSource
  case class HistorySource(history: Date) extends TweetSource

  case class NewObjectInfo(query: String, objectType: String, source: TweetSource)

  object NewObjectInfo {
    val userType = "user"
    val keywordType = "keyword"
  }

  implicit val encodeNewObjectInfo: Encoder[NewObjectInfo] =
    (a: NewObjectInfo) => {
      a.source match {
        case StreamSource =>
          Json.obj(
            ("source", Json.fromString("stream")),
            ("objectType", Json.fromString(a.objectType)),
            ("query", Json.fromString(a.query))
          )
        case HistorySource(history) =>
          Json.obj(
            ("source", Json.fromString("history")),
            ("objectType", Json.fromString(a.objectType)),
            ("history", Json.fromString(history.toUTCString())),
            ("query", Json.fromString(a.query))
          )
      }
    }

  def newObject(credentials: String, tweet: NewObjectInfo): Future[Either[Exception, MessageInfo]] = {
    val url = s"${MainApp.apiUrl}/objects"
    val headers = authorizationHeader(credentials)
    val json = tweet.asJson.noSpaces
    Ajax.post(url, headers = headers, data = InputData.str2ajax(json))
      .map(r => decode[MessageInfo](r.responseText))
      .recoverAjax()
  }

  case class DayStats(date: Date, count: Int)

  case class ObjectStats(id: Int, query: String, deleted: Boolean, allTweets: Int, days: Seq[DayStats])

  implicit val decodeDayStats: Decoder[DayStats] =
    (c: HCursor) => for {
      date <- c.downField("date").as[String]
      count <- c.downField("count").as[Int]
    } yield DayStats(new Date(Date.parse(date)), count)

  def addParamsToUrl(url: String, params: List[(String, String)], first: Boolean = true): String = params match {
    case Nil => url
    case (p, v) :: t => addParamsToUrl(s"$url${if (first) "?" else "&"}$p=$v", t, first = false)
  }

  def objectStats(credentials: String, objectId: Int, from: Option[Date] = None, to: Option[Date] = None): Future[ObjectStats] = {
    val params = List(
      from.map(x => ("from", x.toUTCString())),
      to.map(x => ("to", new Date(x.getTime() + 24 * 60 * 60 * 1000).toUTCString()))
    ).flatten
    val url = addParamsToUrl(s"${MainApp.apiUrl}/objects/$objectId", params)
    val headers = authorizationHeader(credentials)
    Ajax.get(url, headers = headers)
      .map(r => decode[ObjectStats](r.responseText))
      .recoverAjax()
      .flatMap {
        case Right(xs) => Future.successful(xs)
        case Left(e) => Future.failed(e)
      }
  }

  def deleteObject(credentials: String, objectId: Int, all: Boolean): Future[MessageInfo] = {
    val url = addParamsToUrl(s"${MainApp.apiUrl}/objects/$objectId", List(("all", all.show)))
    val headers = authorizationHeader(credentials)
    Ajax.delete(url, headers = headers)
      .map(r => decode[MessageInfo](r.responseText))
      .recoverAjax()
      .flatMap {
        case Right(xs) => Future.successful(xs)
        case Left(e) => Future.failed(e)
      }
  }
}
