package foreshadow.inventory.sheets

import cats.data._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import eu.timepit.refined.auto._
import foreshadow.inventory.core.models._
import gsheets4s.GSheets4s
import gsheets4s.model._

trait GSheetsService[F[_]] {
  def get: F[Map[Barcode, Title]]
  def append(book: Book): F[Unit]
}

class GSheetsServiceImpl[F[_]: Sync](credentials: Ref[F, Credentials]) extends GSheetsService[F] {
  private val spreadsheetID = "1dHXvZjfomTxkx-gce-StFapzhu5acFRYAALybDtCzDs"
  private val gsheets = GSheets4s[F](credentials).spreadsheetsValues
  private val inventoryTable: A1Notation =
//    a1"""Sheet1!A1:B1"""
    RangeNotation(Range(ColRowPosition("A", 1), ColRowPosition("B", 2000)))

  def get: F[Map[Barcode, Title]] = {
    EitherT(gsheets.get(spreadsheetID, inventoryTable))
      .leftMap(GsheetsException(_))
      .map {
        case ValueRange(_, _, values) => values.collect {
          case k :: v :: _ => tagBarcode(k) -> tagTitle(v)
        }.toMap
      }
      .rethrowT
  }

  def append(book: Book): F[Unit] =
    EitherT(gsheets.append(spreadsheetID, inventoryTable, List(List(book.barcode, book.title))))
      .leftMap(GsheetsException(_))
      .rethrowT
      .void

}

object GSheetsService {
  def apply[F[_]: Sync](credentials: Credentials): Resource[F, GSheetsService[F]] =
    Resource.liftF(Ref.of[F, Credentials](credentials)
      .map(ref => new GSheetsServiceImpl[F](ref)))
}

case class GsheetsException(code: Int,
                            message: String,
                            status: String
                           ) extends RuntimeException(message)

object GsheetsException {
  def apply(e: GsheetsError): GsheetsException = GsheetsException(e.code, e.message, e.status)
}
