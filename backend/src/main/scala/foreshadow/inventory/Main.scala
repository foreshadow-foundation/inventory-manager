package foreshadow.inventory

import java.util.Base64

import cats.data._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import foreshadow.inventory.core._
import foreshadow.inventory.core.model._
import foreshadow.inventory.sheets._
import gsheets4s.model.{Credentials => GCreds}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._
import org.http4s.headers.Authorization
import sheets.InventoryService._

import scala.util.Try

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    app[IO].use(_ => IO.never).as(ExitCode.Success)

  def httpRoutes[F[_] : ConcurrentEffect : ContextShift](blocker: Blocker): Resource[F, HttpRoutes[F]] =
    for {
      client <- BlazeClientBuilder[F](blocker.blockingContext).resource
      googleOAuth2 <- GoogleOAuth2.resource[F](client)
      api <- ForeshadowInventoryApi[F](googleOAuth2)
    } yield fileService[F](FileService.Config(".", blocker)) <+> api.service

  def app[F[_] : ConcurrentEffect : Timer : ContextShift]: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      routes <- httpRoutes[F](blocker)
      server <- BlazeServerBuilder[F]
        .bindHttp(23456, "10.37.0.18")
        .withHttpApp(routes.orNotFound)
        .resource
    } yield server
}

object ForeshadowInventoryApi {
  def apply[F[_] : Sync](googleOAuth2: GoogleOAuth2[F]): Resource[F, ForeshadowInventoryApi[F]] =
    Resource.pure[F, ForeshadowInventoryApi[F]](new ForeshadowInventoryApi[F](googleOAuth2))
}

case object MissingAuthorizationHeader extends RuntimeException("Missing Authorization header", null, true, false)
case object InvalidAuthorizationHeader extends RuntimeException("Invalid Authorization header", null, true, false)

class ForeshadowInventoryApi[F[_] : Sync](googleOAuth2: GoogleOAuth2[F]) extends Http4sDsl[F] {
  private implicit def circeDecoder[T: Decoder]: EntityDecoder[F, T] = jsonOf[F, T]

  val authTokensFromRequest: Kleisli[OptionT[F, *], Request[F], GoogleOAuthTokens] =
    Kleisli { req =>
      val maybeHeaderT = req.headers.get(Authorization)
      EitherT.fromEither[F] {
        for {
          header <- maybeHeaderT.toRight(MissingAuthorizationHeader).flatMap {
            case Authorization(Credentials.Token(_, t)) => Right(t)
            case _ => Left(InvalidAuthorizationHeader)
          }
          decoded <- Try(new String(Base64.getDecoder.decode(header), "UTF-8")).toEither
          json <- parse(decoded)
          tokens <- json.as[GoogleOAuthTokens]
        } yield tokens
      }
        .leftSemiflatMap(ex => Sync[F].delay(println(ex.getMessage)))
        .toOption
    }

  val middleware: AuthMiddleware[F, GoogleOAuthTokens] = AuthMiddleware(authTokensFromRequest)

  def authenticatedRoutes: AuthedRoutes[GoogleOAuthTokens, F] = AuthedRoutes.of[GoogleOAuthTokens, F] {
    case GET -> Root / "api" / "known-barcodes" / barcode as GoogleOAuthTokens(accessToken, refreshToken) =>
      for {
        credRef <- Ref.of[F, GCreds](GCreds(accessToken, refreshToken, webappGoogleOauthClientId, Config.webappGoogleOauthClientSecret))
        inventory <- InventoryService[Kleisli[F, Ref[F, GCreds], *]].get(spreadsheetID, inventoryTable).run(credRef)
        GCreds(accessToken, _, _, _) <- credRef.get
        resp <- inventory.get(tagBarcode(barcode)).fold(NotFound(s"could not find $barcode", Header("X-Access-Token", accessToken)))(t => Ok(t.asJson, Header("X-Access-Token", accessToken)))
      } yield resp

    case (req@POST -> Root / "api" / "known-barcodes") as GoogleOAuthTokens(accessToken, refreshToken) =>
      for {
        credRef <- Ref.of[F, GCreds](GCreds(accessToken, refreshToken, webappGoogleOauthClientId, Config.webappGoogleOauthClientSecret))
        input <- req.as[Book]
        _ <- Sync[F].delay(println(s"trying to add $input"))
        _ <- InventoryService[Kleisli[F, Ref[F, GCreds], *]].append(spreadsheetID, inventoryTableHeader)(input).run(credRef)
        GCreds(accessToken, _, _, _) <- credRef.get
        resp <- NoContent(Header("X-Access-Token", accessToken))
      } yield resp
  }

  def anonymousRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req@POST -> Root / "api" / "authentication" / "authorize-code" =>
      for {
        GoogleAuthorizationCodeExchangeRequest(code) <- req.as[GoogleAuthorizationCodeExchangeRequest]
        tokens <- googleOAuth2.exchangeAuthorizationCodeForTokens(code)
        _ <- Sync[F].delay(println(s"got tokens $tokens"))
        resp <- Ok(TupleGeneric[GoogleOAuthTokens].from(tokens).asJson)
      } yield resp
  }

  def service: HttpRoutes[F] = anonymousRoutes <+> middleware(authenticatedRoutes)
}
