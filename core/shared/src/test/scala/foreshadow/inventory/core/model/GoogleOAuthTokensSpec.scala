package foreshadow.inventory.core.model

import io.circe.syntax._
import io.circe._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GoogleOAuthTokensSpec extends AnyFlatSpec with Matchers {

  val exampleTokensObject: GoogleOAuthTokens = GoogleOAuthTokens(tagGoogleAccessToken("access-token"), tagGoogleRefreshToken("refresh-token"))
  val exampleJson: Json = JsonObject(
    "accessToken" -> Json.fromString("access-token"),
    "refreshToken" -> Json.fromString("refresh-token")
  ).asJson

  behavior of "encoding"

  it should "encode to the expected structure" in {
    exampleTokensObject.asJson should be(exampleJson)
  }

  it should "print to the expected structure" in {
    val output = exampleTokensObject.asJson.noSpacesSortKeys

    output should be(exampleJson.noSpacesSortKeys)
    output should be("""{"accessToken":"access-token","refreshToken":"refresh-token"}""")
  }

  behavior of "decoding"

  it should "decode to the expected structure" in {
    exampleJson.as[GoogleOAuthTokens] should be(Right(exampleTokensObject))
  }
}
