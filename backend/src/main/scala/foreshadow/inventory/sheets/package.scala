package foreshadow.inventory

import shapeless.tag
import shapeless.tag._

package object sheets {
  type SpreadsheetId = String @@ SpreadsheetIdTag

  val tagSpreadsheetId: String => SpreadsheetId = tag[SpreadsheetIdTag][String]
}

package sheets {
  trait SpreadsheetIdTag
}
