package foreshadow.inventory

import cats.effect._
import cats.implicits._
import foreshadow.inventory.core._
import foreshadow.inventory.core.model._
import foreshadow.inventory.sheets._
import gsheets4s.model.{Credentials => GCreds, _}
import io.circe._
import io.circe.syntax._
import io.circe.literal._
import org.http4s._
import org.http4s.circe._
import org.http4s.client.blaze._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    app[IO].use(_ => IO.never).as(ExitCode.Success)

  def httpRoutes[F[_] : ConcurrentEffect : ContextShift](blocker: Blocker): Resource[F, HttpRoutes[F]] =
    for {
      sheets <- GSheetsInventoryService[F](googleCredentials, spreadsheetID, inventoryTable)
      client <- BlazeClientBuilder[F](blocker.blockingContext).resource
      googleOAuth2 <- GoogleOAuth2.resource[F](client)
      api <- ForeshadowInventoryApi[F](sheets, googleOAuth2)
    } yield api.service <+> fileService[F](FileService.Config(".", blocker))

  def app[F[_] : ConcurrentEffect : Timer : ContextShift]: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      routes <- httpRoutes[F](blocker)
      server <- BlazeServerBuilder[F]
        .bindHttp(23456, "10.37.0.18")
        .withHttpApp(routes.orNotFound)
        .resource
    } yield server

  private val deviceGoogleOauthClientSecret: String = ???
  private val deviceAccessToken: String = ???
  private val deviceRefreshToken: String = ???
  private val googleCredentials = GCreds(deviceAccessToken, deviceRefreshToken, deviceGoogleOauthClientId, deviceGoogleOauthClientSecret)

  private val spreadsheetID = "1dHXvZjfomTxkx-gce-StFapzhu5acFRYAALybDtCzDs"
  private val inventoryTable: A1Notation = a1"""Sheet1!A1:B1"""
}

object ForeshadowInventoryApi {
  def apply[F[_] : Sync](sheets: InventoryService[F], googleOAuth2: GoogleOAuth2[F]): Resource[F, ForeshadowInventoryApi[F]] =
    Resource.pure[F, ForeshadowInventoryApi[F]](new ForeshadowInventoryApi[F](sheets, googleOAuth2))
}

class ForeshadowInventoryApi[F[_] : Sync](sheets: InventoryService[F], googleOAuth2: GoogleOAuth2[F]) extends Http4sDsl[F] {
  private implicit def circeDecoder[T: Decoder]: EntityDecoder[F, T] = jsonOf[F, T]

  def service: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "api" / "known-barcodes" / barcode =>
      for {
        inventory <- sheets.get
        resp <- inventory.get(tagBarcode(barcode)).fold(NotFound(s"could not find $barcode"))(t => Ok(t.asJson))
      } yield resp

    case req@POST -> Root / "api" / "known-barcodes" =>
      for {
        input <- req.as[Book]
        _ <- Sync[F].delay(println(s"trying to add $input"))
        _ <- sheets.append(input)
        resp <- Ok()
      } yield resp

    case req@POST -> Root / "api" / "authentication" / "authorize-code" =>
      for {
        GoogleAuthorizationCodeExchangeRequest(code) <- req.as[GoogleAuthorizationCodeExchangeRequest]
        tokens <- googleOAuth2.exchangeAuthorizationCodeForTokens(code)
        _ <- Sync[F].delay(println(s"got tokens $tokens"))
        resp <- Ok(TupleGeneric[GoogleOAuthTokensResponse].from(tokens).asJson)
      } yield resp
  }
}
