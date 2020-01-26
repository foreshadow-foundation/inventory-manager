package foreshadow.inventory.sheets

import org.scalatest.flatspec.AnyFlatSpec
import io.circe.literal._
import foreshadow.inventory.core.model._
import org.scalatest.matchers.should.Matchers

class GoogleOAuth2Spec extends AnyFlatSpec with Matchers {

  behavior of "GoogleOAuthTokenResponse"

  it should "correctly decode" in {

    val json =
      json"""{
               "access_token": "access-token",
               "expires_in": 3599,
               "refresh_token": "refresh-token",
               "scope": "openid https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email",
               "token_type": "Bearer",
               "id_token": "id-token"
             }"""

    val output = json.as[GoogleOAuthTokenResponse]

    output should be(Right(
      GoogleOAuthTokenResponse(
        access_token = tagGoogleAccessToken("access-token"),
        refresh_token = tagGoogleRefreshToken("refresh-token"),
        expires_in = Option(3599),
        token_type = Option("Bearer"),
        scope = Option("openid https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"),
        id_token = Option("id-token")
      )
    ))

  }

}
