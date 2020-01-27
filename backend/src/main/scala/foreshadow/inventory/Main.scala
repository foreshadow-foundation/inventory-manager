package foreshadow.inventory

import cats.effect._
import cats.implicits._
import org.http4s.server._
import org.http4s.server.blaze._
import org.http4s.syntax.all._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    app[IO].use(_ => IO.never).as(ExitCode.Success)

  def app[F[_] : ConcurrentEffect : Timer : ContextShift]: Resource[F, Server[F]] =
    for {
      blocker <- Blocker[F]
      routes <- ForeshadowInventoryApi.httpRoutes[F](blocker)
      server <- BlazeServerBuilder[F]
        .bindHttp(23456, "10.37.0.18")
        .withHttpApp(routes.orNotFound)
        .resource
    } yield server
}
