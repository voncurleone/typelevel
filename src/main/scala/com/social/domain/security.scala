package com.social.domain

import tsec.authentication.{AugmentedJWT, JWTAuthenticator, SecuredRequest}
import tsec.mac.jca.HMACSHA256
import com.social.domain.user.*
import org.http4s.Response

object security {
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]] = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
}
