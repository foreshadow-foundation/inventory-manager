package foreshadow.inventory.core

import shapeless.tag
import shapeless.tag.@@

package object models {
  type Barcode = String @@ BarcodeTag
  type Title = String @@ TitleTag

  val tagBarcode: String => Barcode = tag[BarcodeTag][String]
  val tagTitle: String => Title = tag[TitleTag][String]
}

package models {
  trait BarcodeTag
  trait TitleTag
}
