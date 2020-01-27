package foreshadow.inventory.sheets

import cats.data._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.tagless._
import foreshadow.inventory.core.model._
import gsheets4s.GSheets4s
import gsheets4s.model._

@finalAlg
@autoInstrument
@autoFunctorK
trait InventoryService[F[_]] {
  def get(spreadsheetId: SpreadsheetId, table: A1Notation): F[Map[Barcode, Title]]
  def append(spreadsheetId: SpreadsheetId, table: A1Notation)(book: Book): F[Unit]
}

object InventoryService {
  val spreadsheetID: SpreadsheetId = tagSpreadsheetId("1dHXvZjfomTxkx-gce-StFapzhu5acFRYAALybDtCzDs")
  val inventoryTable: A1Notation = a1"""Sheet1!A1:B2000"""
  val inventoryTableHeader: A1Notation = a1"""Sheet1!A1:B1"""

  implicit def GSheetsInventoryServiceInstance[F[_] : Sync]: InventoryService[Kleisli[F, Ref[F, Credentials], *]] = {
    // TODO use mapK to remove the duplicated Kleisli logic
    new InventoryService[Kleisli[F, Ref[F, Credentials], *]] {
      override def get(spreadsheetId: SpreadsheetId, table: A1Notation): Kleisli[F, Ref[F, Credentials], Map[Barcode, Title]] = Kleisli {
        GSheetsInventoryService[F](_).get(spreadsheetId, table)
      }

      override def append(spreadsheetId: SpreadsheetId, table: A1Notation)(book: Book): Kleisli[F, Ref[F, Credentials], Unit] = Kleisli {
        GSheetsInventoryService[F](_).append(spreadsheetId, table)(book)
      }
    }
  }
}

class GSheetsInventoryServiceImpl[F[_]: Sync](credentials: Ref[F, Credentials],
                                             ) extends InventoryService[F] {
  private val gsheets = GSheets4s[F](credentials).spreadsheetsValues

  def get(spreadsheetId: SpreadsheetId, table: A1Notation): F[Map[Barcode, Title]] = {
    EitherT(gsheets.get(spreadsheetId, table))
      .leftMap(GsheetsException(_))
      .map {
        case ValueRange(_, _, values) => values.collect {
          case k :: v :: _ => tagBarcode(k) -> tagTitle(v)
        }.toMap
      }
      .rethrowT
  }

  def append(spreadsheetId: SpreadsheetId, table: A1Notation)(book: Book): F[Unit] =
    EitherT(gsheets.append(spreadsheetId, table, List(List(book.barcode, book.title))))
      .leftMap(GsheetsException(_))
      .rethrowT
      .void

}

object GSheetsInventoryService {
  def apply[F[_]: Sync](credentials: Ref[F, Credentials],
                       ): InventoryService[F] =
      new GSheetsInventoryServiceImpl[F](credentials)
}

case class GsheetsException(code: Int,
                            message: String,
                            status: String
                           ) extends RuntimeException(message)

object GsheetsException {
  def apply(e: GsheetsError): GsheetsException = GsheetsException(e.code, e.message, e.status)
}
