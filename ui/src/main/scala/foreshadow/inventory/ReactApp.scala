package foreshadow.inventory

import org.scalajs.dom
import foreshadow.inventory.css.AppCSS
import foreshadow.inventory.routes.AppRouter

import scala.scalajs.js.annotation.JSExport

object ReactApp {

  @JSExport
  def main(args: Array[String]): Unit = {
    AppCSS.load()
    AppRouter.router().renderIntoDOM(dom.document.getElementById("foreshadow-inventory-app"))

    ()
  }

}
