package foreshadow.inventory.components

import cats.implicits._
import japgolly.scalajs.react.CatsReact._
import japgolly.scalajs.react.Ref.Simple
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.{BackendScope => _}
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import org.scalajs.dom.html
import foreshadow.inventory.core.models._

import scala.concurrent.duration._

object NewBookForm {
  case class Props(addBookToTable: (Barcode, Title) => CallbackTo[Unit],
                   barcodeInput: Simple[html.Input],
                  )

  sealed trait StateMachine
  case class BarcodeEntry(barcodeHolder: String = "") extends StateMachine
  case class TitleLookup(barcode: Barcode) extends StateMachine
  @Lenses case class TitleEntry(barcode: Barcode, titleHolder: String = "") extends StateMachine

  val initialState: StateMachine = BarcodeEntry()

  class Backend($: BackendScope[Props, StateMachine]) {
    def handleSubmitTitle(e: ReactEventFromInput): Callback = {
      val updateState: Props => PartialFunction[StateMachine, Callback] = props => {
        case TitleEntry(barcode, titleHolder) =>
          resetAndAddBook(barcode, tagTitle(titleHolder), props.addBookToTable)
      }

      e.preventDefaultCB >>
        ($.props product $.state) >>= Function.uncurried(updateState).tupled
    }

    val remoteTitleLookup: Barcode => AsyncCallback[Option[Title]] = barcode =>
      AsyncCallback.pure{
        if (barcode == "asdf") Option("the title from the database").map(tagTitle)
        else None
      }.delay(500.millis)

    val titleLookup: Barcode => Callback =
      remoteTitleLookup(_).flatMap { maybeTitle =>
        def updateStateFromLookupResult(props: Props): PartialFunction[(StateMachine, Option[Title]), Callback] = {
          case (TitleLookup(barcode), Some(title)) =>
            resetAndAddBook(barcode, title, props.addBookToTable)
          case (TitleLookup(barcode), None) =>
            $.setState(TitleEntry(barcode))
        }

        (for {
          props <- $.props
          state <- $.state
          _ <- updateStateFromLookupResult(props)((state, maybeTitle))
        } yield ()).asAsyncCallback
      }.toCallback

    def handleSubmitBarcode(e: ReactEventFromInput): Callback = {
      val barcodeEntryToTitleLookup: PartialFunction[StateMachine, StateMachine] = {
        case BarcodeEntry(b) => TitleLookup(tagBarcode(b))
      }

      val getBarcode: PartialFunction[StateMachine, Barcode] = {
        case BarcodeEntry(barcodeHolder) => tagBarcode(barcodeHolder)
      }

      e.preventDefaultCB >>
        $.modState(barcodeEntryToTitleLookup) >> $.state.map(getBarcode) >>= titleLookup
    }

    def handleNewBarcodeOnChange(e: ReactEventFromInput): CallbackTo[Unit] = {
      val value = e.target.value
      val setBarcodeEntry: PartialFunction[StateMachine, StateMachine] = {
        case BarcodeEntry(_) => BarcodeEntry(value)
      }

      $.modState(setBarcodeEntry)
    }

    def handleTitleOnChange(e: ReactEventFromInput): CallbackTo[Unit] = {
      val value = e.target.value
      val setTitleEntry: PartialFunction[StateMachine, StateMachine] = {
        case t: TitleEntry => TitleEntry.titleHolder.set(value)(t)
      }

      $.modState(setTitleEntry)
    }

    def resetAndAddBook(barcode: Barcode, title: Title, addBookToTable: (Barcode, Title) => CallbackTo[Unit]): Callback =
      addBookToTable(barcode, title) >> $.setState(initialState)

    def render(props: Props, state: StateMachine): VdomElement = state match {
      case BarcodeEntry(barcodeHolder) =>
        <.form(^.onSubmit ==> handleSubmitBarcode,
          <.label("Barcode:",
            <.input.text(
              ^.onChange ==> handleNewBarcodeOnChange,
              ^.value := barcodeHolder,
            ).withRef(props.barcodeInput),
          ),
        )
      case TitleLookup(barcode) =>
        <.div(s"Checking title for $barcode…")
      case TitleEntry(barcode, titleHolder) =>
        <.form(^.onSubmit ==> handleSubmitTitle,
          <.label(s"Title for $barcode:",
            <.input.text(
              ^.onChange ==> handleTitleOnChange,
              ^.value := titleHolder,
            ),
          ),
        )
    }
  }

  val component = ScalaComponent
    .builder[Props]("NewBookForm")
    .initialState(initialState)
    .renderBackend[Backend]
    .build

  def apply(props: Props): VdomElement =
    component(props).vdomElement
}
