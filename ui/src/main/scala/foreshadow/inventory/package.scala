package foreshadow

import cats.effect.ContextShift
import hammock.InterpTrans
import japgolly.scalajs.react.AsyncCallback
import japgolly.scalajs.react.effects.AsyncCallbackEffects._
import hammock._

import scala.concurrent.ExecutionContext

package object inventory {
  implicit val asyncCallbackContextShift: ContextShift[AsyncCallback] = new ContextShift[AsyncCallback] {
    override def shift: AsyncCallback[Unit] = AsyncCallback.unit
    override def evalOn[A](ec: ExecutionContext)(fa: AsyncCallback[A]): AsyncCallback[A] = fa
  }
  implicit val interpreter: InterpTrans[AsyncCallback] = hammock.js.Interpreter.instance[AsyncCallback]

  val serverBaseUri = uri"http://johnston.cryingtreeofmercury.com:23456"
}
