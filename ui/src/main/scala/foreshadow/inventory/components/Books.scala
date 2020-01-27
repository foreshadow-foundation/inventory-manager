package foreshadow.inventory.components

import japgolly.scalajs.react.Ref.Simple
import japgolly.scalajs.react.component.Scala.{BackendScope => _, _}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, _}
import monocle.macros.Lenses
import org.scalajs.dom.html
import scalacss.DevDefaults._
import scalacss.ScalaCssReact._
import cats.implicits._
import foreshadow.inventory.core.model._

object Books {

  object Style extends StyleSheet.Inline {
    import dsl._

    val table: StyleA = style(textAlign.center,
      width(100.pc),
      minHeight(10.em),
    )

    val container: StyleA = style(display.flex,
      flexDirection.column,
      listStyle := "none",
      padding.`0`)

  }

  @Lenses case class State(items: Seq[BookRow.Book])

  object State {
    val appendNewBook: (Barcode, Title) => State => State = (newBookBarCode, title) => s =>
      State.items.set(s.items :+ BookRow.Book(newBookBarCode, Option(title)))(s)
  }

  val initialState: State = State(
    items = Vector(
      "0043951097" -> "The Ninth Nugget",
      "0060293152" -> "The Two Princesses of Bamarre",
    ).map(_.bimap(tagBarcode, tagTitle).map(Option(_)))
      .map(BookRow.Book.tupled)
  )

  class Backend($: BackendScope[GoogleOAuthTokens, State]) {
    private val barcodeInput: Simple[html.Input] = Ref[html.Input]

    val focusOnBarcodeEntry: Callback = barcodeInput.foreach(_.focus())

    val handleSubmit: Book => CallbackTo[Unit] = {
      case Book(barcode, title) =>
        $.modState(State.appendNewBook(barcode, title), focusOnBarcodeEntry)
    }

    def render(tokens: GoogleOAuthTokens, state: State) =
      <.div(
        <.table(Style.table,
          <.thead(
            <.tr(
              <.th("Barcode"),
              <.th("Title"),
            ),
          ),
          <.tbody(
            state.items.map(BookRow.apply): _*
          ),
        ),
        NewBookForm(NewBookForm.Props(handleSubmit, barcodeInput, tokens)),
      )
  }

  val component: Component[GoogleOAuthTokens, State, Backend, CtorType.Props] = ScalaComponent
    .builder[GoogleOAuthTokens]("Books")
    .initialState(initialState)
    .renderBackend[Backend]
    .componentDidMount(_.backend.focusOnBarcodeEntry)
    .build

  def apply(accessToken: GoogleAccessToken, refreshToken: GoogleRefreshToken): VdomElement = component(GoogleOAuthTokens(accessToken, refreshToken))
}
