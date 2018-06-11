package twitter.shared

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object PaginationView {

  case class Props(itemsCount: Int,
                   currentPage: Int,
                   pageSize: Int,
                   onPageChange: Int => Callback)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      val pageCount = (p.itemsCount + p.pageSize - 1) / p.pageSize
      val first = if (p.currentPage == 0) "disabled" else "waves-effect"
      val last = if (p.currentPage + 1 == pageCount)  "disabled" else "waves-effect"
      <.ul(^.className := "pagination",
        <.li(^.className := first,
          <.a(^.href := "#!", ^.onClick --> (if (p.currentPage == 0) Callback.empty else p.onPageChange(p.currentPage - 1)),
            <.i(^.className := "material-icons", "chevron_left")
          )
        ),
        (0 until pageCount).toTagMod { i =>
          <.li(^.className := (if (i == p.currentPage) "active" else "waves-effect"),
            ^.onClick --> (if (i == p.currentPage) Callback.empty else p.onPageChange(i)),
            <.a(^.href := "#!", i + 1)
          )
        },
        <.li(^.className := last,
          <.a(^.href := "#!", ^.onClick --> (if (p.currentPage + 1 == pageCount) Callback.empty else p.onPageChange(p.currentPage + 1)),
            <.i(^.className := "material-icons", "chevron_right")
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("PaginationView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)
}
