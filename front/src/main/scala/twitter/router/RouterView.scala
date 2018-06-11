package twitter.router

import diode.react.ModelProxy
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import twitter.Models
import twitter.login.{LoginView, Logout}
import twitter.tweets.TweetsView

object RouterView {

  case class Props(proxy: ModelProxy[Models])

  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {
      val proxy = p.proxy()
      <.div(^.className := "main",
        <.header(
          <.nav(^.className := "teal",
            <.div(^.className := "nav-wrapper container",
              <.a(^.className := "brand-logo", "Twitter Saver"),
              <.ul(^.className := "right",
                proxy.tweetsModel.credentials match {
                  case Some(_) =>
                    <.li(
                      <.a(^.href := "#!", ^.onClick --> p.proxy.dispatchCB(Logout),
                        "Wyloguj",
                        <.i(^.className := "material-icons right", "exit_to_app")
                      )
                    )
                  case None => EmptyVdom
                }
              )
            )
          )
        ),
        <.main(
          <.div(^.className := "container",
            proxy.tweetsModel.credentials match {
              case Some(_) => p.proxy.wrap(_.tweetsModel)(x => TweetsView(x(), x.dispatchCB))
              case None => p.proxy.wrap(_.loginModel)(x => LoginView(x(), x.dispatchCB))
            }
          )
        ),
        <.footer(^.className := "page-footer teal",
          <.div(^.className := "footer-copyright",
            <.div(^.className := "container",
              "Piotr Krzeszewski, Łukasz Ławniczak, Artur Minorczyk © 2018 - Budowa systemu analizy danych"
            )
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("RouterView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[Models]) =
    component(Props(proxy))
}