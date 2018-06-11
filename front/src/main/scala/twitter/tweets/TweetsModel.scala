package twitter.tweets

import diode.data.PotState._
import diode.data.{Empty, Pot, PotAction}
import diode.{Action, ActionHandler, Effect, ModelRW}
import twitter.tweets.TweetsRest.{NewObjectInfo, ObjectStats, StreamSource, TweetListItem, TweetSource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Date

case class TweetsModel(credentials: Option[String],
                       tweets: Pot[Seq[TweetListItem]],
                       newObjectInfo: NewObjectInfo,
                       newObjectProgress: Boolean,
                       newObjectError: Option[String],
                       selectedObjectId: Option[Int],
                       listCurrentPage: Int,
                       objectStats: Pot[ObjectStats],
                       objectStatsFrom: Option[Date],
                       objectStatsTo: Option[Date],
                       statsCurrentPage: Int,
                       objectDeleteAll: Boolean)

object TweetsModel {
  val initModel = TweetsModel(
    credentials = None,
    tweets = Pot.empty,
    newObjectInfo = NewObjectInfo("", "keyword", StreamSource),
    newObjectProgress = false,
    newObjectError = None,
    selectedObjectId = None,
    listCurrentPage = 0,
    objectStats = Pot.empty,
    objectStatsFrom = None,
    objectStatsTo = None,
    statsCurrentPage = 0,
    objectDeleteAll = false,
  )
}

case class LoginSuccessful(credentials: String) extends Action
case object ClearCredentials extends Action

case class UpdateTweetsList(potResult: Pot[Seq[TweetListItem]] = Empty) 
  extends PotAction[Seq[TweetListItem], UpdateTweetsList] {
  override def next(newResult: Pot[Seq[TweetListItem]]) = UpdateTweetsList(newResult)
}
case class SelectObject(id: Int) extends Action
case class ListPageChanged(page: Int) extends Action

case class ObjectQueryChanged(query: String) extends Action
case class ObjectTypeChanged(tweetType: String) extends Action
case class ObjectSourceChanged(tweetSource: TweetSource) extends Action
case object ObjectSubmit extends Action
case class NewObjectError(error: String) extends Action
case object NewObjectSuccessful extends Action

case class UpdateObjectStats(objectId: Int,
                             from: Option[Date] = None,
                             to: Option[Date] = None,
                             potResult: Pot[ObjectStats] = Empty,
                            ) extends PotAction[ObjectStats, UpdateObjectStats] {
  override def next(newResult: Pot[ObjectStats]): UpdateObjectStats = UpdateObjectStats(objectId, from, to, newResult)
}
case class ObjectStatsFromChanged(date: Option[Date]) extends Action
case class ObjectStatsToChanged(date: Option[Date]) extends Action
case class StatsPageChanged(page: Int) extends Action
case class ObjectDeleteAllChanged(all: Boolean) extends Action
case class ObjectDelete(objectId: Int) extends Action

class TweetsHandler[M](modelRW: ModelRW[M, TweetsModel]) extends ActionHandler(modelRW) {
  override def handle = {
    case LoginSuccessful(credentials) =>
      val effect = Effect(Future.successful(UpdateTweetsList()))
      updated(TweetsModel.initModel.copy(credentials = Some(credentials)), effect)
    case ClearCredentials =>
      updated(value.copy(credentials = None))
    case action: UpdateTweetsList =>
      val effect = action.effect(TweetsRest.tweetsList(value.credentials.getOrElse("")))(identity)
      action.handle {
        case PotEmpty =>
          updated(value.copy(tweets = value.tweets.pending()), effect)
        case PotPending =>
          noChange
        case PotReady =>
          updated(value.copy(tweets = action.potResult))
        case PotUnavailable =>
          updated(value.copy(tweets = value.tweets.unavailable()))
        case PotFailed =>
          val e = action.result.failed.get
          updated(value.copy(tweets = value.tweets.fail(e)))
      }
    case SelectObject(id) =>
      value.selectedObjectId match {
        case Some(i) if i == id =>
          updated(value.copy(
            selectedObjectId = None,
            objectStats = Pot.empty
          ))
        case _ =>
          val effect = Effect(Future.successful(UpdateObjectStats(id)))
          updated(value.copy(
            selectedObjectId = Some(id),
            objectStatsFrom = None,
            objectStatsTo = None,
            objectDeleteAll = false
          ), effect)
      }
    case ListPageChanged(p) =>
      updated(value.copy(listCurrentPage = p))
    case ObjectQueryChanged(v) =>
      updated(value.copy(newObjectInfo = value.newObjectInfo.copy(query = v)))
    case ObjectTypeChanged(v) =>
      updated(value.copy(newObjectInfo = value.newObjectInfo.copy(objectType = v)))
    case ObjectSourceChanged(v) =>
      updated(value.copy(newObjectInfo = value.newObjectInfo.copy(source = v)))
    case ObjectSubmit =>
      val effect = Effect(TweetsRest.newObject(value.credentials.getOrElse(""), value.newObjectInfo)
        .map {
          case Left(e) => NewObjectError(e.getMessage)
          case Right(_) => NewObjectSuccessful
        })
      updated(value.copy(newObjectProgress = true), effect)
    case NewObjectError(e) =>
      updated(value.copy(newObjectProgress = false, newObjectError = Some(e)))
    case NewObjectSuccessful =>
      val effect = Effect(Future.successful(UpdateTweetsList()))
      updated(value.copy(
        newObjectInfo = TweetsModel.initModel.newObjectInfo,
        newObjectProgress = false,
        newObjectError = None
      ), effect)
    case action: UpdateObjectStats =>
      val effect = action.effect(
        TweetsRest.objectStats(value.credentials.getOrElse(""), action.objectId, action.from, action.to)
      )(identity)
      action.handle {
        case PotEmpty =>
          updated(value.copy(objectStats = value.objectStats.pending()), effect)
        case PotPending =>
          noChange
        case PotReady =>
          if (value.selectedObjectId.contains(action.potResult.get.id)) {
            updated(value.copy(objectStats = action.potResult))
          } else {
            noChange
          }
        case PotUnavailable =>
          updated(value.copy(objectStats = value.objectStats.unavailable()))
        case PotFailed =>
          val e = action.result.failed.get
          updated(value.copy(objectStats = value.objectStats.fail(e)))
      }
    case ObjectStatsFromChanged(date) =>
      val effect = Effect(Future.successful(UpdateObjectStats(
        objectId = value.selectedObjectId.getOrElse(0),
        from = date,
        to = value.objectStatsTo
      )))
      updated(value.copy(objectStatsFrom = date), effect)
    case ObjectStatsToChanged(date) =>
      val effect = Effect(Future.successful(UpdateObjectStats(
        objectId = value.selectedObjectId.getOrElse(0),
        from = value.objectStatsFrom,
        to = date
      )))
      updated(value.copy(objectStatsTo = date), effect)
    case StatsPageChanged(p) =>
      updated(value.copy(statsCurrentPage = p))
    case ObjectDeleteAllChanged(all) =>
      updated(value.copy(objectDeleteAll = all))
    case ObjectDelete(id) =>
      val isDeleted = value.objectStats.toOption.exists(_.deleted)
      val effect = Effect(Future.successful(SelectObject(id))) >>
        Effect(TweetsRest.deleteObject(value.credentials.getOrElse(""), id, value.objectDeleteAll || isDeleted)
          .map(_ => UpdateTweetsList()))
      updated(value, effect)
  }
}
