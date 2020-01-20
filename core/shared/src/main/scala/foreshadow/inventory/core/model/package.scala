package foreshadow.inventory.core

import io.circe._
import cats.implicits._
import shapeless.tag
import shapeless.tag.@@

package object models {
  type Barcode = String @@ BarcodeTag
  type Title = String @@ TitleTag

  val tagBarcode: String => Barcode = tag[BarcodeTag][String]
  val tagTitle: String => Title = tag[TitleTag][String]

  implicit val barcodeEncoder: Encoder[Barcode] = Encoder[String].narrow
  implicit val barcodeDecoder: Decoder[Barcode] = Decoder[String].map(tagBarcode)
  implicit val titleEncoder: Encoder[Title] = Encoder[String].narrow
  implicit val titleDecoder: Decoder[Title] = Decoder[String].map(tagTitle)
}

package models {
  trait BarcodeTag
  trait TitleTag

  case class Book(barcode: Barcode,
                  title: Title)

  object Book {
    implicit val bookCodec: Codec[Book] = io.circe.generic.semiauto.deriveCodec
  }
}
