/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

final case class Authorization(tokenType: String, accessToken: String)

object Authorization:
    val TokenTypeBearer: String = "Bearer"
    val TokenTypeApiKey: String = "APIKey"

    def bearer(accessToken: String): Authorization = Authorization(TokenTypeBearer, accessToken)
