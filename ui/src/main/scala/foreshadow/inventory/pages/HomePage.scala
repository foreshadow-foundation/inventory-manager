package foreshadow.inventory
package pages

import cats.implicits._
import foreshadow.inventory.components.Books
import foreshadow.inventory.core._
import foreshadow.inventory.core.model._
import hammock._
import japgolly.scalajs.react._
import japgolly.scalajs.react.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ScalaCssReact._

object HomePage {

  object Style extends StyleSheet.Inline {
    import dsl._
    val content = style(textAlign.center,
                        minHeight(450.px),
                        paddingTop(40.px))
  }

  case class Props(code: Option[GoogleAuthorizationCode])

  sealed trait State
  case object Anonymous extends State
  case class AuthorizationCode(code: GoogleAuthorizationCode) extends State
  case class Authenticated(accessToken: GoogleAccessToken, refreshToken: GoogleRefreshToken) extends State

  class Backend(val $: BackendScope[Props, State]) {

    val exchangeTokens: Callback =
      for {
        _ <- Callback {
          org.scalajs.dom.window.history.replaceState("", "", "/index.html")
        }
        state <- $.state
        _ <- state match {
          case AuthorizationCode(code) =>
            import hammock._
            import hammock.circe.implicits._
            import hammock.marshalling._

            (for {
              GoogleOAuthTokens(a, r) <- Hammock.request(Method.POST, serverBaseUri / "api" / "authentication" / "authorize-code", Map("content-type" -> "application/json"), Option(GoogleAuthorizationCodeExchangeRequest(code)))
                .as[GoogleOAuthTokens]
                .exec[AsyncCallback]
              _ <- $.setState(Authenticated(a, r)).asAsyncCallback
            } yield ()).toCallback

          case _ => Callback.empty
        }
      } yield ()

    def render(state: State): VdomElement = state match {
      case AuthorizationCode(code) =>
        <.div(Style.content,
          s"Found code $code; exchanging it for tokens"
        )
      case Anonymous =>
        <.div(Style.content,
          <.h1("Login with Google"),
          <.a(
            ^.href := (uri"https://accounts.google.com/o/oauth2/v2/auth" ? (
              "client_id" -> webappGoogleOauthClientId &
                "redirect_uri" -> (serverBaseUri / "index.html").show &
                "include_granted_scopes" -> true.show &
                "response_type" -> "code" &
                "scope" -> scope &
                "prompt" -> "consent" &
                "access_type" -> "offline"
              )).show,
            "Login"
          ),
        )
      case Authenticated(accessToken, refreshToken) =>
        <.div(Style.content,
          <.h1("Foreshadow Foundation Inventory Management"),
//          <.div(s"Authenticated as $accessToken, $refreshToken"),
          Books(accessToken, refreshToken),
        )
    }
  }

  val buildStateFromProps: Props => State = {
    case Props(Some(code)) => AuthorizationCode(code)
    case _ => Anonymous
  }

  val component = ScalaComponent
    .builder[Props]("HomePage")
    .initialStateFromProps(buildStateFromProps)
    .renderBackend[Backend]
    .componentDidMount(_.backend.exchangeTokens)
    .build

  def apply(code: Option[GoogleAuthorizationCode] = None): VdomElement =
    component(Props(code)).vdomElement

}
