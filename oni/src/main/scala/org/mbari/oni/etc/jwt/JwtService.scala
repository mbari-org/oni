/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWT
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

case class JwtService(issuer: String, apiKey: String, signingSecret: String):

    private val algorithm = Algorithm.HMAC512(signingSecret)

    private val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    def verify(jwt: String): Boolean =
        try
            verifier.verify(jwt)
            true
        catch case e: Exception => false

    def authorize(providedApiKey: String): Option[String] =
        if providedApiKey == apiKey then
            val now      = Instant.now()
            val tomorrow = now.plus(1, ChronoUnit.DAYS)
            val iat      = Date.from(now)
            val exp      = Date.from(tomorrow)

            val jwt = JWT
                .create()
                .withIssuer(issuer)
                .withIssuedAt(iat)
                .withExpiresAt(exp)
                .sign(algorithm)
            Some(jwt)
        else None
