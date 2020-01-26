package foreshadow.inventory
package pages

import cats.implicits._
import foreshadow.inventory.components.Books
import foreshadow.inventory.core._
import foreshadow.inventory.core.model._
import foreshadow.inventory.routes.AppRouter.AppPage
import japgolly.scalajs.react._
import japgolly.scalajs.react.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ScalaCssReact._
import hammock._

object HomePage {

  object Style extends StyleSheet.Inline {
    import dsl._
    val content = style(textAlign.center,
                        minHeight(450.px),
                        paddingTop(40.px))
  }

  case class Props(code: Option[GoogleAuthorizationCode], routerCtl: RouterCtl[AppPage])

  sealed trait State
  case object Anonymous extends State
  case class AuthorizationCode(code: GoogleAuthorizationCode) extends State
  case class Authenticated(accessToken: GoogleAccessToken, refreshToken: GoogleRefreshToken) extends State

  class Backend(val $: BackendScope[Props, State]) {

    val exchangeTokens: Callback = $.state.flatMap {
      case AuthorizationCode(code) =>
        import hammock._
        import hammock.circe.implicits._
        import hammock.marshalling._

        (for {
          GoogleOAuthTokensResponse(a, r) <- Hammock.request(Method.POST, serverBaseUri / "api" / "authentication" / "authorize-code", Map("content-type" -> "application/json"), Option(GoogleAuthorizationCodeExchangeRequest(code)))
            .as[GoogleOAuthTokensResponse]
            .exec[AsyncCallback]
          _ <- $.setState(Authenticated(a, r)).asAsyncCallback
        } yield ()).toCallback

      case _ => Callback.empty
    }

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
          <.div(s"Authenticated as $accessToken, $refreshToken"),
          Books(),
        )
    }
  }

  val buildStateFromProps: Props => State = {
    case Props(Some(code), _) => AuthorizationCode(code)
    case _ => Anonymous
  }

  val component = ScalaComponent
    .builder[Props]("HomePage")
    .initialStateFromProps(buildStateFromProps)
    .renderBackend[Backend]
    .componentWillMount(_.backend.exchangeTokens)
    .build

  def apply(routerCtl: RouterCtl[AppPage], code: Option[GoogleAuthorizationCode] = None): VdomElement =
    component(Props(code, routerCtl)).vdomElement

}
/*
https://accounts.google.com/o/oauth2/v2/auth?
 scope=https%3A//www.googleapis.com/auth/drive.metadata.readonly&
 access_type=offline&
 include_granted_scopes=true&
 state=state_parameter_passthrough_value&
 redirect_uri=https%3A//oauth2.example.com/callback&
 response_type=code&
 client_id=client_id

https://accounts.google.com/o/oauth2/v2/auth?
scope=openid%20https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/spreadsheets%20https://www.googleapis.com/auth/userinfo.email&
access_type=offline&
redirect_uri=http://johnston.cryingtreeofmercury.com:23456/index.html&
client_id=70342818130-esaovok28pps753r8vievbspnrmvkg5l.apps.googleusercontent.com&
response_type=code
 */
