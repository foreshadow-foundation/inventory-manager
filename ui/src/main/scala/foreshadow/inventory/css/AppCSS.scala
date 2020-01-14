package foreshadow.inventory.css

import foreshadow.inventory.components._
import foreshadow.inventory.pages._
import scalacss.DevDefaults._
import scalacss.internal.mutable.GlobalRegistry

object AppCSS {

  def load() = {
    GlobalRegistry.register(GlobalStyle,
                            TopNav.Style,
                            HomePage.Style,
                            Books.Style,
    )
    GlobalRegistry.onRegistration(_.addToDocument())
  }
}
