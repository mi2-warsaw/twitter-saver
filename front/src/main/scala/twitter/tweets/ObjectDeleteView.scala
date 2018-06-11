package twitter.tweets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js

object ObjectDeleteView {

  case class Props(query: String,
                   isDeleted: Boolean,
                   deleteCheck: Boolean,
                   onDeleteCheck: Boolean => Callback,
                   onSubmit: Callback)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      <.div(^.className := "modal", ^.id := "delete-modal",
        <.div(^.className := "modal-content",
          <.h4("Potwierdź usunięcie"),
          <.p(s"Czy na pewno chcesz usunąć obiekt ", <.i(p.query), " z listy śledzonych obiektów?"),
          <.p(
            <.label(
              <.input(^.`type` := "checkbox", ^.checked := p.deleteCheck, ^.onChange --> p.onDeleteCheck(!p.deleteCheck)),
              <.span("Usuń permanentnie")
            )
          ).when(!p.isDeleted)
        ),
        <.div(^.className := "modal-footer",
          <.a(^.className := "modal-action modal-close waves-effect waves-green btn-flat", ^.href := "#!",
            ^.onClick --> p.onSubmit, "Tak"),
          <.a(^.className := "modal-action modal-close waves-effect waves-red btn-flat", ^.href := "#!", "Nie")
        )
      )
    }

    private val M = js.Dynamic.global.M

    val start = Callback {
      val elem = dom.document.querySelector(".modal")
      M.Modal.init(elem)
    }
  }

  val component = ScalaComponent.builder[Props]("ObjectDeleteView")
    .stateless
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .build

  def apply(props: Props) = component(props)
}
