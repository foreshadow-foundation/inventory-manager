package foreshadow.inventory.sheets

import cats.effect._
import cats.implicits._
import cats.tagless.{autoFunctorK, autoInstrument, finalAlg}
import foreshadow.inventory.core.model._

@finalAlg
@autoInstrument
@autoFunctorK
trait Config[F[_]] {
  def webappGoogleOauthClientSecret: F[GoogleClientSecret]
}

object Config {
  def getEnv[F[_] : Sync](key: String): F[String] =
    Sync[F].delay(sys.env.get(key).toRight(MissingEnvironment(key))) >>= (_.liftTo[F])

  implicit def configFromSystemEnvironmentInstance[F[_] : Sync]: Config[F] = new Config[F] {
    override def webappGoogleOauthClientSecret: F[GoogleClientSecret] =
      getEnv("GOOGLE_CLIENT_SECRET").map(tagGoogleClientSecret)
  }
}

case class MissingEnvironment(key: String) extends RuntimeException(s"Missing environment variable $key", null, true, false)
