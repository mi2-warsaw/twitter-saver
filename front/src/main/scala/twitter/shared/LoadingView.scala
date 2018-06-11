package twitter.shared

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object LoadingView {

  case class Props()

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      <.div(^.className := "preloader-wrapper big active",
        <.div(^.className := "spinner-layer spinner-blue-only",
          <.div(^.className := "circle-clipper left",
            <.div(^.className := "circle")
          ),
          <.div(^.className := "gap-patch",
            <.div(^.className := "circle")
          ),
          <.div(^.className := "circle-clipper right",
            <.div(^.className := "circle")
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("LoadingView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply() = component(Props())
}
