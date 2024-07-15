/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.jwt

class JwtServiceSuite extends munit.FunSuite {

    val jwtService = JwtService("issueer", "apiKey", "signingSecret")

    test("authorize") {
        val opt = jwtService.authorize(jwtService.apiKey)
        assert(opt.isDefined)
    }

    test("verify") {
        val opt = jwtService.authorize(jwtService.apiKey)
        assert(opt.isDefined)
        val jwt = opt.get
        val verfied = jwtService.verify(jwt)
        assert(verfied)
    }

  
}
