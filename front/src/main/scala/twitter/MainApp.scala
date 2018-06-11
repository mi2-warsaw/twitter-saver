package twitter

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import twitter.login.TryInitialLogin
import twitter.router.RouterView

object MainApp {

  val apiUrl = "/api"

  def main(args: Array[String]): Unit = {
    val modelsConnection = AppCircuit.connect(_.models)
    val routerView = modelsConnection(proxy => RouterView(proxy))
    routerView.renderIntoDOM(dom.document.getElementsByClassName("app")(0).domAsHtml)

    AppCircuit.dispatch(TryInitialLogin)
  }
}
