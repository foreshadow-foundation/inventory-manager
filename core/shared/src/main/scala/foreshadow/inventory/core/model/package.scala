package foreshadow.inventory.core

import cats.Show
import io.circe._
import cats.implicits._
import shapeless.tag
import shapeless.tag.@@

package object model {
  type Barcode = String @@ BarcodeTag
  type Title = String @@ TitleTag
  type GoogleAccessToken = String @@ GoogleAccessTokenTag
  type GoogleRefreshToken = String @@ GoogleRefreshTokenTag
  type GoogleAuthorizationCode = String @@ GoogleAuthorizationCodeTag
  type GoogleClientId = String @@ GoogleClientIdTag
  type GoogleClientSecret = String @@ GoogleClientSecretTag

  val tagGoogleAuthorizationCode: String => GoogleAuthorizationCode = tag[GoogleAuthorizationCodeTag][String]
  val tagGoogleClientId: String => GoogleClientId = tag[GoogleClientIdTag][String]
  val tagGoogleClientSecret: String => GoogleClientSecret = tag[GoogleClientSecretTag][String]

  val tagBarcode: String => Barcode = tag[BarcodeTag][String]
  val tagTitle: String => Title = tag[TitleTag][String]
  val tagGoogleAccessToken: String => GoogleAccessToken = tag[GoogleAccessTokenTag][String]
  val tagGoogleRefreshToken: String => GoogleRefreshToken = tag[GoogleRefreshTokenTag][String]

  implicit val barcodeEncoder: Encoder[Barcode] = Encoder[String].narrow
  implicit val barcodeDecoder: Decoder[Barcode] = Decoder[String].map(tagBarcode)
  implicit val titleEncoder: Encoder[Title] = Encoder[String].narrow
  implicit val titleDecoder: Decoder[Title] = Decoder[String].map(tagTitle)
  implicit val googleAccessTokenEncoder: Encoder[GoogleAccessToken] = Encoder[String].narrow
  implicit val googleAccessTokenDecoder: Decoder[GoogleAccessToken] = Decoder[String].map(tagGoogleAccessToken)
  implicit val googleRefreshTokenEncoder: Encoder[GoogleRefreshToken] = Encoder[String].narrow
  implicit val googleRefreshTokenDecoder: Decoder[GoogleRefreshToken] = Decoder[String].map(tagGoogleRefreshToken)
  implicit val googleAuthorizationCodeEncoder: Encoder[GoogleAuthorizationCode] = Encoder[String].narrow
  implicit val googleAuthorizationCodeDecoder: Decoder[GoogleAuthorizationCode] = Decoder[String].map(tagGoogleAuthorizationCode)
  implicit val googleClientIdEncoder: Encoder[GoogleClientId] = Encoder[String].narrow
  implicit val googleClientIdDecoder: Decoder[GoogleClientId] = Decoder[String].map(tagGoogleClientId)
  implicit val googleClientSecretEncoder: Encoder[GoogleClientSecret] = Encoder[String].narrow
  implicit val googleClientSecretDecoder: Decoder[GoogleClientSecret] = Decoder[String].map(tagGoogleClientSecret)

  implicit def taggedStringShow[T]: Show[String @@ T] = (t: String @@ T) => t
}

package model {

  import io.circe.generic.semiauto.deriveCodec

  trait BarcodeTag
  trait TitleTag
  trait GoogleAccessTokenTag
  trait GoogleRefreshTokenTag
  trait GoogleAuthorizationCodeTag
  trait GoogleClientIdTag
  trait GoogleClientSecretTag

  case class Book(barcode: Barcode,
                  title: Title)

  case class GoogleAuthorizationCodeExchangeRequest(code: GoogleAuthorizationCode)
  object GoogleAuthorizationCodeExchangeRequest {
    implicit def googleAuthorizationCodeExchangeRequestCodec: Codec[GoogleAuthorizationCodeExchangeRequest] = deriveCodec
  }

  case class GoogleOAuthTokensResponse(accessToken: GoogleAccessToken, refreshToken: GoogleRefreshToken)
  object GoogleOAuthTokensResponse {
    implicit def googleOAuthTokensResponseCodec: Codec[GoogleOAuthTokensResponse] = deriveCodec
  }

  object Book {
    implicit val bookCodec: Codec[Book] = deriveCodec
  }
}
