package foreshadow.inventory.components

import japgolly.scalajs.react.component.Scala.{BackendScope => _, _}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, _}
import foreshadow.inventory.core.models._

object BookRow {
  case class Book(barcode: Barcode, title: Option[Title])

  val component: Component[Book, Unit, Unit, CtorType.Props] =
    ScalaComponent.builder[Book]("BookRow")
      .render_P((b: Book) =>
        <.tr(
          <.td(b.barcode),
          <.td(b.title.getOrElse[String]("")),
        )
      )
      .build

  def apply(props: Book): VdomElement = component(props).vdomElement
}
