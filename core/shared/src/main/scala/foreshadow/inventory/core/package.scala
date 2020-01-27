package foreshadow.inventory

import foreshadow.inventory.core.model._

package object core {
  val webappGoogleOauthClientId: GoogleClientId = tagGoogleClientId("70342818130-esaovok28pps753r8vievbspnrmvkg5l.apps.googleusercontent.com")

  val deviceGoogleOauthClientId: GoogleClientId = tagGoogleClientId("70342818130-mpdo2a3eb9bfrnt3kj8tu9kradh2p4t2.apps.googleusercontent.com")
  val scope = "openid https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/userinfo.email"
}
