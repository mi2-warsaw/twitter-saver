package twitter.tweets

import cats.Functor
import cats.implicits._
import diode.Action
import diode.data.Pot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement
import twitter.AppCircuit
import twitter.shared.{DateUtils, LoadingView, PaginationView}
import twitter.tweets.TweetsRest.ObjectStats

import scala.scalajs.js
import scala.scalajs.js.Date

object ObjectView {

  case class Props(objectStats: Pot[ObjectStats],
                   from: Option[Date],
                   to: Option[Date],
                   currentPage: Int,
                   onFromChange: Date => Callback,
                   onToChange: Date => Callback,
                   onPageChange: Int => Callback)

  class Backend($: BackendScope[Props, Unit]) {
    def render(p: Props): VdomElement = {
      <.div(^.className := "card",
        <.div(^.className := "card-content",
          if (p.objectStats.nonEmpty) {
            val pageSize = 10
            val objectStats = p.objectStats.get
            val days = objectStats.days.sortBy(-_.date.getTime()).slice(p.currentPage * pageSize, (p.currentPage + 1) * pageSize)
            val tweetsCount = objectStats.days.foldLeft(0)((acc, x) => acc + x.count)
            val showDate: Option[Date] => Option[String] = Functor[Option].lift(x => DateUtils.showDate(x))
            <.div(
              <.h5(^.className := "title", s"Zarządzaj obiektem ", <.i(objectStats.query)),
              <.div(
                <.div(^.className := "input-field no-padding-left col s6",
                  <.input(^.className := "datepicker", ^.`type` := "text", ^.id := "stats-from",
                    ^.value := showDate(p.from).getOrElse(""), ^.disabled := p.objectStats.isPending),
                  <.label(^.`for` := "stats-from", "Od")
                ),
                <.div(^.className := "input-field no-padding-right col s6",
                  <.input(^.className := "datepicker", ^.`type` := "text", ^.id := "stats-to",
                    ^.value := showDate(p.to).getOrElse(""), ^.disabled := p.objectStats.isPending),
                  <.label(^.`for` := "stats-to", "Do")
                )
              ),
              <.p(s"Sumaryczna liczba tweetów: ${objectStats.allTweets}"),
              <.p(s"Liczba tweetów w wybranym okresie: $tweetsCount"),
              if (objectStats.days.nonEmpty) {
                <.div(
                  <.table(^.className := "bordered",
                    <.thead(
                      <.tr(
                        <.th("Data"),
                        <.th("Liczba tweetów")
                      )
                    ),
                    <.tbody(
                      days.toTagMod { d =>
                        <.tr(
                          <.td(d.date.toDateString()),
                          <.td(d.count)
                        )
                      }
                    )
                  ),
                  PaginationView(PaginationView.Props(
                    itemsCount = objectStats.days.size,
                    currentPage = p.currentPage,
                    pageSize = pageSize,
                    onPageChange = page => p.onPageChange(page)
                  ))
                )
              } else {
                <.br
              },
              <.a(^.className := "waves-effect waves-light btn red modal-trigger", ^.href := "#delete-modal", ^.disabled := p.objectStats.isPending, "Usuń obiekt")
            )
          } else {
            EmptyVdom
          },
          if (p.objectStats.isPending) {
            <.div(^.className := "center loading",
              LoadingView()
            )
          } else {
            EmptyVdom
          },
          p.objectStats.exceptionOption.whenDefined { e =>
            <.div(^.className := "center loading red-text", e.getMessage)
          }
        )
      )
    }

    private val M = js.Dynamic.global.M

    def update = Callback {
      def findElem(selector: String): HTMLInputElement = {
        dom.document.querySelector(selector).asInstanceOf[HTMLInputElement]
      }
      val findElemOpt: Option[String] => Option[HTMLInputElement] = Functor[Option].lift(x => findElem(x))
      def elemDate(selector: Option[String]): Option[Date] = {
        findElemOpt(selector).flatMap(elem => DateUtils.parseDate(elem.value).toOption)
      }
      def updateDatepicker(elem: HTMLInputElement, action: Option[Date] => Action, minSelector: Option[String] = None,
                           maxSelector: Option[String] = None): Unit = {
        if (elem != null) {
          val min = elemDate(minSelector)
          val max = elemDate(maxSelector)
          M.Datepicker.init(elem, js.Dynamic.literal(
            format = "dd-mm-yyyy",
            firstDay = 1,
            i18n = DateUtils.i18n,
            showClearBtn = true,
            onClose = () => {
              val date = DateUtils.parseDate(elem.value)
              AppCircuit.dispatch(action(date.toOption))
            },
            minDate = min.orNull,
            maxDate = DateUtils.min(max.getOrElse(new Date), new Date())
          ))

          val date = DateUtils.parseDate(elem.value)
          val instance = M.Datepicker.getInstance(elem)
          instance.setDate(date.getOrElse(new Date()))
        }
      }

      val fromElem = findElem("#stats-from")
      updateDatepicker(fromElem, x => ObjectStatsFromChanged(x), maxSelector = Some("#stats-to"))
      val toElem = findElem("#stats-to")
      updateDatepicker(toElem, x => ObjectStatsToChanged(x), minSelector = Some("#stats-from"))
      M.updateTextFields()
    }
  }

  val component = ScalaComponent.builder[Props]("ObjectView")
    .stateless
    .renderBackend[Backend]
    .componentDidUpdate(_.backend.update)
    .build

  def apply(props: Props) = component(props)
}
