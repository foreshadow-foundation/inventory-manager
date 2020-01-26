package foreshadow.inventory.sheets

import cats._
import cats.effect._
import cats.implicits._
import foreshadow.inventory.core.model._
import foreshadow.inventory.core._
import cats.tagless._
import io.circe._
import org.http4s._
import org.http4s.client._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.client.middleware.Logger
import org.http4s.syntax.all._

@autoInstrument
@autoFunctorK
trait GoogleOAuth2[F[_]] {
  def exchangeAuthorizationCodeForTokens(code: GoogleAuthorizationCode): F[(GoogleAccessToken, GoogleRefreshToken)]
}

case class GoogleOAuthTokenExchange(code: GoogleAuthorizationCode,
                                    clientId: GoogleClientId,
                                    clientSecret: GoogleClientSecret,
                                    redirectUri: Uri,
                                    grantType: GrantType,
                                   ) {

  def toUrlForm: UrlForm = UrlForm(
    "code" -> code.show,
    "client_id" -> clientId.show,
    "client_secret" -> clientSecret.show,
    "redirect_uri" -> redirectUri.renderString,
    "grant_type" -> grantType.show,
  )
}

sealed trait GrantType
case object AuthorizationCode extends GrantType

object GrantType {
  implicit def grantTypeEncoder: Encoder[GrantType] = Encoder[String].contramap {
    case AuthorizationCode => "authorization_code"
  }

  implicit def grantTypeShow: Show[GrantType] = {
    case AuthorizationCode => "authorization_code"
  }
}

case class GoogleOAuthTokenResponse(access_token: GoogleAccessToken,
                                    refresh_token: GoogleRefreshToken,
                                    expires_in: Option[Int],
                                    token_type: Option[String],
                                    scope: Option[String],
                                    id_token: Option[String],
                                   )

object GoogleOAuthTokenResponse {
  implicit def googleOAuthTokenResponseCodec: Codec[GoogleOAuthTokenResponse] = io.circe.generic.semiauto.deriveCodec
  implicit def googleOAuthTokenResponseEntityDecoder[F[_] : Sync]: EntityDecoder[F, GoogleOAuthTokenResponse] = jsonOf[F, GoogleOAuthTokenResponse]
}

object GoogleOAuth2 {
  def resource[F[_] : Concurrent](client: Client[F]): Resource[F, GoogleOAuth2[F]] =
    Resource.pure[F, GoogleOAuth2[F]](new GoogleOAuth2Impl[F](Logger(logHeaders = true, logBody = true)(client)))
}

private[sheets] class GoogleOAuth2Impl[F[_] : Sync](client: Client[F]) extends GoogleOAuth2[F] with Http4sClientDsl[F] {
  override def exchangeAuthorizationCodeForTokens(code: GoogleAuthorizationCode): F[(GoogleAccessToken, GoogleRefreshToken)] =
    for {
      req <- POST(GoogleOAuthTokenExchange(
        code = code,
        clientId = webappGoogleOauthClientId,
        clientSecret = webappGoogleOauthClientSecret,
        redirectUri = uri"http://johnston.cryingtreeofmercury.com:23456/index.html",
        AuthorizationCode,
      ).toUrlForm, uri"https://oauth2.googleapis.com/token")
      resp <- client.expect[GoogleOAuthTokenResponse](req)
      _ <- Sync[F].delay(println(s"got response $resp"))
    } yield (resp.access_token, resp.refresh_token)
}
