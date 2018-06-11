package twitter

import diode.Circuit
import diode.react.ReactConnector
import twitter.login.{LoginHandler, LoginModel}
import twitter.tweets.{TweetsHandler, TweetsModel}

case class AppModel(models: Models)

case class Models(loginModel: LoginModel, tweetsModel: TweetsModel)

object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {

  override protected def initialModel: AppModel = AppModel(
    Models(
      LoginModel.initModel,
      TweetsModel.initModel
    )
  )

  override protected def actionHandler: AppCircuit.HandlerFunction = composeHandlers(
    new LoginHandler(zoomTo(_.models.loginModel)),
    new TweetsHandler(zoomTo(_.models.tweetsModel))
  )
}