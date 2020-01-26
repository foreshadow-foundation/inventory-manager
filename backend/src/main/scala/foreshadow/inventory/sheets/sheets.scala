package foreshadow.inventory.sheets

import cats.data._
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import foreshadow.inventory.core.model._
import gsheets4s.GSheets4s
import gsheets4s.model._

trait InventoryService[F[_]] {
  def get: F[Map[Barcode, Title]]
  def append(book: Book): F[Unit]
}

class GSheetsInventoryServiceImpl[F[_]: Sync](credentials: Ref[F, Credentials],
                                              spreadsheetId: String,
                                              inventoryTable: A1Notation,
                                             ) extends InventoryService[F] {
  private val gsheets = GSheets4s[F](credentials).spreadsheetsValues

  def get: F[Map[Barcode, Title]] = {
    EitherT(gsheets.get(spreadsheetId, inventoryTable))
      .leftMap(GsheetsException(_))
      .map {
        case ValueRange(_, _, values) => values.collect {
          case k :: v :: _ => tagBarcode(k) -> tagTitle(v)
        }.toMap
      }
      .rethrowT
  }

  def append(book: Book): F[Unit] =
    EitherT(gsheets.append(spreadsheetId, inventoryTable, List(List(book.barcode, book.title))))
      .leftMap(GsheetsException(_))
      .rethrowT
      .void

}

object GSheetsInventoryService {
  def apply[F[_]: Sync](credentials: Credentials,
                        spreadsheetId: String,
                        inventoryTable: A1Notation,
                       ): Resource[F, InventoryService[F]] =
    Resource.liftF(Ref.of[F, Credentials](credentials)
      .map(ref => new GSheetsInventoryServiceImpl[F](ref, spreadsheetId, inventoryTable)))


}

case class GsheetsException(code: Int,
                            message: String,
                            status: String
                           ) extends RuntimeException(message)

object GsheetsException {
  def apply(e: GsheetsError): GsheetsException = GsheetsException(e.code, e.message, e.status)
}
