package org.mbari.oni.endpoints

import org.mbari.oni.domain.{ConceptNameCreate, ConceptNameTypes, Page, RawConcept}
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.UserAuthMixin
import sttp.model.StatusCode
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}

trait ConceptNameEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin {

    given jwtService: JwtService = JwtService("mbari", "foo", "bar")
    lazy val endpoints: ConceptNameEndpoints = ConceptNameEndpoints(entityManagerFactory)
    private val password = "foofoo"

    test("findAll") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val expected = rawRoot.descendantNames.sorted
        runGet(
            endpoints.allEndpointImpl,
            "http://test.com/v1/names",
            response => {
                assertEquals(response.code, StatusCode.Ok)
                val conceptNames = checkResponse[Page[Seq[String]]](response.body).content.sorted
                assertEquals(conceptNames, expected)
            }
        )
    }

    test("addConceptName") {
        val root = init(3, 3)
        assert(root != null)
        val rawRoot = RawConcept.from(root)
        val name = rawRoot.primaryName
        val dto = ConceptNameCreate(name = name, newName = "newName", nameType = ConceptNameTypes.PRIMARY.getType)

        val attempt = testWithUserAuth( user =>
            runPost(
                endpoints.addConceptNameEndpointImpl,
                "http://test.com/v1/names",
                dto.stringify,
                response => {
                    assertEquals(response.code, StatusCode.Ok)
                    val rawConcept = checkResponse[RawConcept](response.body)
                    val obtained = rawConcept.names.map(_.name).toSeq
                    assert(obtained.contains(dto.name))
                    assert(obtained.contains(dto.newName))
                    println(s"obtained: $obtained")
                },
                jwt = jwtService.login(user.username, password, user.toEntity)
            )
        , Some(password))

        attempt match {
            case Right(_) => println("Success")
            case Left(error) => fail(error.toString)
        }
    }

    test("updateConceptName") {}

    test("deleteConceptName") {}


}