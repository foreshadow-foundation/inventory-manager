package foreshadow.inventory
package components

import java.util.Base64

import cats.Applicative
import cats.data._
import cats.implicits._
import foreshadow.inventory.core.model._
import hammock._
import hammock.circe.implicits._
import hammock.marshalling._
import japgolly.scalajs.react.Ref.Simple
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.Scala.{BackendScope => _}
import japgolly.scalajs.react.effects.AsyncCallbackEffects._
import japgolly.scalajs.react.effects.CallbackToEffects._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.macros.Lenses
import org.scalajs.dom.html
import io.circe.syntax._
import org.scalajs.dom.ext.AjaxException

object NewBookForm {
  case class Props(addBookToTable: Book => CallbackTo[Unit],
                   barcodeInput: Simple[html.Input],
                   tokens: GoogleOAuthTokens,
                  )

  sealed trait StateMachine {
    val tokens: GoogleOAuthTokens
  }
  @Lenses case class BarcodeEntry(barcodeHolder: String = "", tokens: GoogleOAuthTokens) extends StateMachine
  case class TitleLookup(barcode: Barcode, tokens: GoogleOAuthTokens) extends StateMachine
  @Lenses case class TitleEntry(barcode: Barcode, titleHolder: String = "", tokens: GoogleOAuthTokens) extends StateMachine

  val initialState: Props => StateMachine = p => BarcodeEntry(tokens = p.tokens)

  val authorizationHeaderFromTokens: GoogleOAuthTokens => String = tokens => s"Bearer ${Base64.getEncoder.encodeToString(tokens.asJson.noSpaces.getBytes("UTF-8"))}"

  class Backend($: BackendScope[Props, StateMachine]) {
    private val titleEntryInput: Simple[html.Input] = Ref[html.Input]

    def handleSubmitTitle(e: ReactEventFromInput): Callback = {
      val updateState: Props => PartialFunction[StateMachine, Callback] = props => {
        case TitleEntry(barcode, titleHolder, tokens) =>
          resetAndAddBook(Book(barcode, tagTitle(titleHolder)), tokens, Kleisli(props.addBookToTable).mapF(_.asAsyncCallback))
      }

      e.preventDefaultCB >>
        ($.props product $.state) >>= Function.uncurried(updateState).tupled
    }

    val postNewBook: Kleisli[AsyncCallback, (Book, GoogleOAuthTokens), Unit] = Kleisli { case (book, tokens) =>
      Callback(println(s"sending $book")).asAsyncCallback >> {
        val authHeader: (String, String) = "Authorization" -> authorizationHeaderFromTokens(tokens)
        val headers = Map(authHeader)

        Hammock.request(Method.POST, serverBaseUri / "api" / "known-barcodes", headers, Option(book))
          .as[None.type]
          .exec[AsyncCallback]
          .attempt
          .void
      }
    }

    val remoteTitleLookup: (Barcode, GoogleOAuthTokens) => AsyncCallback[Option[Title]] = (barcode, tokens) => {
      Hammock.request(Method.GET, serverBaseUri / "api" / "known-barcodes" / barcode, Map("Authorization" -> authorizationHeaderFromTokens(tokens)))
        .as[String]
        .exec[AsyncCallback]
        .attemptT
        .map(Option(_).map(tagTitle))
        .leftSemiflatMap {
          case t@AjaxException(xhr) if xhr.status != 404 =>
            Callback {
              org.scalajs.dom.window.console.error(t)
            }.asAsyncCallback
          case _ => Applicative[AsyncCallback].unit
        }
        .getOrElse(None)
    }

    val titleLookup: (Barcode, GoogleOAuthTokens) => Callback =
      remoteTitleLookup(_, _).flatMap { maybeTitle =>
        def updateStateFromLookupResult(props: Props): PartialFunction[(StateMachine, Option[Title]), Callback] = {
          case (TitleLookup(barcode, tokens), Some(title)) =>
            resetAndAddBook(Book(barcode, title), tokens, Kleisli(props.addBookToTable).mapF(_.asAsyncCallback))
          case (TitleLookup(barcode, tokens), None) =>
            $.setState(TitleEntry(barcode, tokens = tokens), titleEntryInput.foreach(_.focus()))
        }

        (for {
          props <- $.props
          state <- $.state
          _ <- updateStateFromLookupResult(props)((state, maybeTitle))
        } yield ()).asAsyncCallback
      }.toCallback

    def handleSubmitBarcode(e: ReactEventFromInput): Callback = {
      val barcodeEntryToTitleLookup: PartialFunction[StateMachine, StateMachine] = {
        case BarcodeEntry(b, tokens) => TitleLookup(tagBarcode(b), tokens)
      }

      val getBarcode: PartialFunction[StateMachine, (Barcode, GoogleOAuthTokens)] = {
        case BarcodeEntry(barcodeHolder, tokens) => (tagBarcode(barcodeHolder), tokens)
      }

      e.preventDefaultCB >>
        $.modState(barcodeEntryToTitleLookup) >> $.state.map(getBarcode) >>= titleLookup.tupled
    }

    def handleNewBarcodeOnChange(e: ReactEventFromInput): CallbackTo[Unit] = {
      val value = e.target.value
      val setBarcodeEntry: PartialFunction[StateMachine, StateMachine] = {
        case b: BarcodeEntry => BarcodeEntry.barcodeHolder.set(value)(b)
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

    def setInitialState(tokens: GoogleOAuthTokens): Callback =
      $.setState(BarcodeEntry(tokens = tokens))

    def resetAndAddBook(book: Book, tokens: GoogleOAuthTokens, addBookToTable: Kleisli[AsyncCallback, Book, Unit]): Callback =
      for {
        _ <- List(postNewBook, addBookToTable.local[(Book, GoogleOAuthTokens)](_._1)).parTraverse_(_.run((book, tokens))).toCallback
        _ <- setInitialState(tokens)
      } yield ()

    def render(props: Props, state: StateMachine): VdomElement = state match {
      case BarcodeEntry(barcodeHolder, _) =>
        <.form(^.onSubmit ==> handleSubmitBarcode,
          <.label("Barcode:",
            <.input.text(
              ^.onChange ==> handleNewBarcodeOnChange,
              ^.value := barcodeHolder,
            ).withRef(props.barcodeInput),
          ),
        )
      case TitleLookup(barcode, _) =>
        <.div(s"Checking title for $barcode…")
      case TitleEntry(barcode, titleHolder, _) =>
        <.div(
          <.form(^.onSubmit ==> handleSubmitTitle,
            <.label(s"Title for $barcode:",
              <.input.text(
                ^.onChange ==> handleTitleOnChange,
                ^.value := titleHolder,
              ).withRef(titleEntryInput),
            ),
          ),
          // TODO the input field doesn't focus after clicking on this button
          <.button(^.onClick ==> (_ => setInitialState(state.tokens)),
            "Cancel"
          )
        )
    }
  }

  val component = ScalaComponent
    .builder[Props]("NewBookForm")
    .initialStateFromProps(initialState)
    .renderBackend[Backend]
    .build

  def apply(props: Props): VdomElement =
    component(props).vdomElement
}
