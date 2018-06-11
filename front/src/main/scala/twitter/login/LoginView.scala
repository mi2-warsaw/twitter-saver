package twitter.login

import diode.Action
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object LoginView {

  case class Props(loginModel: LoginModel, dispatch: Action => Callback)

  class Backend($: BackendScope[Props, Unit]) {
    private def submit(dispatch: Action => Callback)(e: ReactEvent) = e.preventDefaultCB >>
        dispatch(LoginSubmit) >>
        CallbackTo.pure(false)

    def render(p: Props): VdomElement = {
      <.div(^.className := "row",
        <.div(^.className := "col s6 offset-s3",
          <.div(^.className := "card-panel login-panel",
            <.form(^.onSubmit ==> submit(p.dispatch),
              <.div(^.className := "row",
                <.div(^.className := "col s12")
              ),
              <.div(^.className := "row",
                <.div(^.className := "input-field col s12",
                  <.input(^.className := "validate", ^.`type` := "text", ^.name := "username", ^.id := "username",
                    ^.required := true, ^.disabled := p.loginModel.progress, ^.onChange ==> { e: ReactEventFromInput =>
                      p.dispatch(UsernameChanged(e.target.value))
                    }),
                  <.label(^.`for` := "username", "Nazwa użytkownika")
                )
              ),
              <.div(^.className := "row",
                <.div(^.className := "input-field col s12",
                  <.input(^.className := "validate", ^.`type` := "password", ^.name := "password", ^.id := "password",
                    ^.required := true, ^.disabled := p.loginModel.progress, ^.onChange ==> { e: ReactEventFromInput =>
                      p.dispatch(PasswordChanged(e.target.value))
                    }),
                  <.label(^.`for` := "password", "Hasło")
                )
              ),
              p.loginModel.error.whenDefined { e =>
                <.div(^.className := "row",
                  <.p(^.className := "red-text", e)
                )
              },
              <.div(^.className := "row",
                <.button(^.className := "col s12 btn btn-large waves-effect", ^.`type` := "submit",
                  ^.name := "btn_login", ^.disabled := p.loginModel.progress, "Zaloguj")
              )
            )
          )
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("LoginView")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(loginModel: LoginModel, dispatch: Action => Callback) =
    component(Props(loginModel, dispatch))
}
