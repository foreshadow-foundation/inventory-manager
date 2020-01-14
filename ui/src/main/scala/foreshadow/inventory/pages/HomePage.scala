package foreshadow.inventory.pages

import scalacss.DevDefaults._
import scalacss.ScalaCssReact._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import foreshadow.inventory.components.Books

object HomePage {

  object Style extends StyleSheet.Inline {
    import dsl._
    val content = style(textAlign.center,
//                        fontSize(30.px),
                        minHeight(450.px),
                        paddingTop(40.px))
  }

  val component =
    ScalaComponent.builder
      .static("HomePage")(
        <.div(Style.content,
          <.h1("Foreshadow Foundation Inventory Management"),
          Books(),
        )
      )
      .build

  def apply() = component()
}
