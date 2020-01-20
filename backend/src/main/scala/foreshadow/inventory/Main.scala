package foreshadow.inventory

import cats.effect._
import cats.implicits._
import foreshadow.inventory.sheets._
import foreshadow.inventory.core.models._
import gsheets4s.model.{Credentials => GCreds}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.server.staticcontent._
import org.http4s.syntax.all._
import org.http4s.circe._
import io.circe.syntax._
import io.circe._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    app[IO].use(_ => IO.never).as(ExitCode.Success)

  def httpRoutes[F[_] : Effect : ContextShift](blocker: Blocker): Resource[F, HttpRoutes[F]] =
    for {
      sheets <- GSheetsService[F](GCreds(accessToken, refreshToken, googleOauthClientId, googleOauthClientSecret))
      api <- ForeshadowInventoryApi[F](sheets)
    } yield api.service <+> fileService[F](FileService.Config(".", blocker))

  def app[F[_] : ConcurrentEffect : Timer : ContextShift]: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      routes <- httpRoutes[F](blocker)
      server <- BlazeServerBuilder[F]
        .bindHttp(23456)
        .withHttpApp(routes.orNotFound)
        .resource
    } yield server

  val googleOauthClientId = "70342818130-mpdo2a3eb9bfrnt3kj8tu9kradh2p4t2.apps.googleusercontent.com"
  val googleOauthClientSecret: String = ???
  val accessToken: String = ???
  val refreshToken: String = ???
  val scope = "openid https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/userinfo.email"
}

object ForeshadowInventoryApi {
  def apply[F[_] : Sync](sheets: GSheetsService[F]): Resource[F, ForeshadowInventoryApi[F]] =
    Resource.pure[F, ForeshadowInventoryApi[F]](new ForeshadowInventoryApi[F](sheets))
}

class ForeshadowInventoryApi[F[_] : Sync](sheets: GSheetsService[F]) extends Http4sDsl[F] {
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
  }
}
