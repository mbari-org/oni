/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

/**
 * Authorization information as snake_case
 *
 * @param token_type
 *   Typically it will be `Beaerer`
 * @param access_token
 *   For Oni this will be a JWT
 */
final case class AuthorizationSC(token_type: String, access_token: String)

object AuthorizationSC:
    val TokenTypeBearer: String = "Bearer"
    val TokenTypeApiKey: String = "APIKey"

    def bearer(accessToken: String): AuthorizationSC = AuthorizationSC(TokenTypeBearer, accessToken)
