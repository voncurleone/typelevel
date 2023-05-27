package com.social.fixtures

import cats.data.OptionT
import cats.effect.IO
import com.social.domain.security.{Authenticator, JwtToken}
import com.social.domain.user.User
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.Authorization
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256

import concurrent.duration.DurationInt

trait SecuredRouteFixture extends UserFixture {
  val mockedAuthenticator: Authenticator[IO] = {
    //key for hashing
    val key = HMACSHA256.unsafeGenerateKey

    //identity store to retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == personEmail) OptionT.pure(person)
      else if (email == adminEmail) OptionT.pure(admin)
      else OptionT.none[IO, User]

    //jwt authenticator
    JWTAuthenticator.unbacked.inBearerToken(
      //expiry of token, max idle optional, idStore, key
      1.day,
      None,
      idStore,
      key
    )
  }

  extension (request: Request[IO]) {
    def withBearerToken(jwtToken: JwtToken): Request[IO] =
      request.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](jwtToken.jwt)
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
  }
}
