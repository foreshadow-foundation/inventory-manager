package foreshadow.inventory.routes

import foreshadow.inventory.components._
import foreshadow.inventory.models.Menu
import foreshadow.inventory.pages.HomePage

import japgolly.scalajs.react.extra.router.{
  Resolution,
  RouterConfigDsl,
  RouterCtl,
  _
}
import japgolly.scalajs.react.vdom.html_<^._

object AppRouter {

  sealed trait AppPage

  case object Home extends AppPage

  val config = RouterConfigDsl[AppPage].buildConfig { dsl =>
    import dsl._
    (trimSlashes
      | staticRoute(root, Home) ~> render(HomePage()))
      .notFound(redirectToPage(Home)(SetRouteVia.HistoryReplace))
      .renderWith(layout)
  }

  val mainMenu = Vector(
    Menu("Home", Home),
  )

  def layout(c: RouterCtl[AppPage], r: Resolution[AppPage]) =
    <.div(
      TopNav(TopNav.Props(mainMenu, r.page, c)),
      r.render(),
    )

  val baseUrl = BaseUrl.fromWindowOrigin / "index.html"

  val router = Router(baseUrl, config)
}
