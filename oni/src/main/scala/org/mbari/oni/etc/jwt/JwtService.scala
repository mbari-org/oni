/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWT
import org.mbari.oni.domain.{UserAccount, UserAccountRoles}
import org.mbari.oni.jpa.entities.UserAccountEntity

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

case class JwtService(issuer: String, apiKey: String, signingSecret: String):

    private val algorithm = Algorithm.HMAC512(signingSecret)

    private val verifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    /**
     * Verify a JWT
     * @param jwt
     *   the JWT to verify
     * @return
     *   true if the JWT is valid, false otherwise
     */
    def verify(jwt: String): Boolean =
        try
            verifier.verify(jwt)
            true
        catch case e: Exception => false

    /**
     * Authorize a request with an API key
     * @param providedApiKey
     *   the API key provided by the client
     * @return
     *   a JWT if the API key is valid, None otherwise
     */
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

    /**
     * Login a user and return a JWT
     * @param name
     *   the username
     * @param pwd
     *   the plain text, non-encrypted password
     * @param entity
     *   the user account entity (looked up from the database)
     * @return
     *   a JWT if the login is successful, None otherwise
     */
    def login(name: String, pwd: String, entity: UserAccountEntity): Option[String] =
        if name == entity.getUserName
            && entity.authenticate(pwd)
            && (entity.isAdministrator || entity.isMaintainer)
        then
            val now      = Instant.now()
            val tomorrow = now.plus(1, ChronoUnit.DAYS)
            val iat      = Date.from(now)
            val exp      = Date.from(tomorrow)

            val jwt = JWT
                .create()
                .withIssuer(issuer)
                .withIssuedAt(iat)
                .withExpiresAt(exp)
                .withSubject(Option(entity.getId).getOrElse(-1).toString)
                .withClaim("name", name)
                .withClaim("role", entity.getRole)
                .sign(algorithm)
            Some(jwt)
        else None

    /**
     * Decode a JWT and return UserAccount info from it's claims
     * @param userJwt
     *   the JWT to decode
     * @return
     *   a UserAccount if the JWT is valid, None otherwise
     */
    def decode(userJwt: String): Option[UserAccount] =
        try
            val decoded = verifier.verify(userJwt)
            val name    = decoded.getClaim("name").asString()
            val role    = decoded.getClaim("role").asString()
            Some(UserAccount(name, "", role))
        catch case e: Exception => None
