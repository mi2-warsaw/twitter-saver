package twitter.tweets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement
import twitter.AppCircuit
import twitter.shared.DateUtils
import twitter.tweets.TweetsRest.{HistorySource, NewObjectInfo, StreamSource, TweetSource}

import scala.scalajs.js
import scala.scalajs.js.Date

object AddObjectView {

  case class Props(info: NewObjectInfo,
                   progress: Boolean,
                   error: Option[String],
                   onQueryChange: String => Callback,
                   onTypeChange: String => Callback,
                   onSourceChange: TweetSource => Callback,
                   onSubmit: Callback)

  class Backend($: BackendScope[Props, Unit]) {
    private def submit(onSubmit: Callback)(e: ReactEvent) = e.preventDefaultCB >>
      onSubmit >>
      CallbackTo.pure(false)

    def render(p: Props): VdomElement = {
      val objectType = p.info.objectType
      val source = p.info.source
      <.div(^.className := "card",
        <.div(^.className := "card-content",
          <.form(^.onSubmit ==> submit(p.onSubmit),
            <.h5(^.className := "title", "Nowy obiekt"),
            <.div(^.className := "radio-group",
              <.label(
                <.input(^.`type` := "radio", ^.checked := objectType == NewObjectInfo.keywordType, ^.disabled := p.progress,
                  ^.onChange --> p.onTypeChange(NewObjectInfo.keywordType)),
                <.span("Słowo kluczowe")
              ),
              <.label(
                <.input(^.`type` := "radio", ^.checked := objectType == NewObjectInfo.userType, ^.disabled := p.progress,
                  ^.onChange --> p.onTypeChange(NewObjectInfo.userType)),
                <.span("Użytkownik")
              )
            ),
            <.div(^.className := "input-field",
              <.input(^.`type` := "text", ^.id := "query", ^.required := true, ^.disabled := p.progress,
                ^.value := p.info.query, ^.onChange ==> { e: ReactEventFromInput =>
                  p.onQueryChange(e.target.value)
                }),
              <.label(^.`for` := "query", if (objectType == NewObjectInfo.keywordType) "Słowo kluczowe" else "Użytkownik")
            ),
            <.div(^.className := "radio-group",
              <.label(
                <.input(^.`type` := "radio", ^.checked := source == StreamSource, ^.disabled := p.progress,
                  ^.onChange --> p.onSourceChange(StreamSource)),
                <.span("Stream")
              ),
              <.label(
                <.input(^.`type` := "radio", ^.checked := source != StreamSource, ^.disabled := p.progress,
                  ^.onChange --> p.onSourceChange(HistorySource(DateUtils.nowWithoutTime()))),
                <.span("Batch")
              )
            ),
            p.info.source match {
              case HistorySource(date) =>
                <.div(^.className := "input-field",
                  <.input(^.className := "datepicker", ^.`type` := "text", ^.id := "add-datepicker", ^.disabled := p.progress,
                    ^.defaultValue := DateUtils.showDate(date)),
                  <.label(^.`for` := "add-datepicker", "Pobierz historię od")
                )
              case StreamSource => EmptyVdom
            },
            p.error.whenDefined { e =>
              <.p(^.className := "red-text", e)
            },
            <.button(^.className := "btn waves-effect", ^.`type` := "submit", ^.disabled := p.progress, "Dodaj")
          )
        )
      )
    }

    private val M = js.Dynamic.global.M

    def update = Callback {
      val elem = dom.document.querySelector("#add-datepicker").asInstanceOf[HTMLInputElement]
      if (elem != null) {
        val date = DateUtils.parseDate(elem.value)
        M.Datepicker.init(elem, js.Dynamic.literal(
          format = "dd-mm-yyyy",
          firstDay = 1,
          i18n = DateUtils.i18n,
          onClose = () => {
            val date = DateUtils.parseDate(elem.value)
            AppCircuit.dispatch(ObjectSourceChanged(HistorySource(date.getOrElse(new Date))))
          },
          minDate = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000),
          maxDate = new Date()
        ))
        val instance = M.Datepicker.getInstance(elem)
        instance.setDate(date.getOrElse(new Date()))
      }
      M.updateTextFields()
    }
  }

  val component = ScalaComponent.builder[Props]("AddObjectView")
    .stateless
    .renderBackend[Backend]
    .componentDidUpdate(_.backend.update)
    .build

  def apply(props: Props) = component(props)
}
