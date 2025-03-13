/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.endpoints

import org.mbari.oni.domain.{Media, MediaCreate, MediaTypes, MediaUpdate}
import org.mbari.oni.etc.circe.CirceCodecs.{*, given}
import org.mbari.oni.etc.jdk.Strings
import org.mbari.oni.etc.jwt.JwtService
import org.mbari.oni.jdbc.FastPhylogenyService
import org.mbari.oni.jpa.DataInitializer
import org.mbari.oni.services.UserAuthMixin
import sttp.model.StatusCode

import java.net.URI
import scala.jdk.CollectionConverters.*

trait MediaEndpointsSuite extends EndpointsSuite with DataInitializer with UserAuthMixin:

    given jwtService: JwtService       = JwtService("mbari", "foo", "bar")
    lazy val fastPhylogenyService      = FastPhylogenyService(entityManagerFactory)
    lazy val endpoints: MediaEndpoints = MediaEndpoints(entityManagerFactory, fastPhylogenyService)
    private val password               = Strings.random(10)

    def createMedia(): Seq[Media] =
        val root = init(1, 6)
        root.getDescendants
            .asScala
            .flatMap(_.getConceptMetadata.getMedias.asScala)
            .toSeq
            .map(Media.from)
            .sortBy(_.url.toExternalForm)

    test("mediaForConcept") {
        val expected = createMedia()
        val opt      = expected.head.conceptName
        assert(opt.isDefined)
        val name     = opt.get
        runGet(
            endpoints.mediaForConceptEndpointImpl,
            s"http://test.com/v1/media/${name}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Seq[Media]](response.body)
                    .sortBy(_.url.toExternalForm)
                assertEquals(obtained, expected)
        )
    }

    test("findMediaById") {
        val media = createMedia().head
        assert(media.id.isDefined)
        runGet(
            endpoints.findMediaByIdEndpointImpl,
            s"http://test.com/v1/media/${media.id.get}",
            response =>
                assertEquals(response.code, StatusCode.Ok)
                val obtained = checkResponse[Media](response.body)
                assertEquals(obtained, media)
        )
    }

    test("createMedia") {
        val root        = init(2, 0)
        assert(root != null)
        val mediaCreate = MediaCreate(
            conceptName = root.getName,
            url = URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL,
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val attempt     = testWithUserAuth(
            user =>
                runPost(
                    endpoints.createMediaEndpointImpl,
                    "http://test.com/v1/media",
                    mediaCreate.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[Media](response.body)
                        assertEquals(mediaCreate.conceptName, obtained.conceptName.getOrElse(""))
                        assertEquals(mediaCreate.url, obtained.url)
                        assertEquals(mediaCreate.caption, obtained.caption)
                        assertEquals(mediaCreate.credit, obtained.credit)
                        val t        = Media.resolveMimeType(mediaCreate.mediaType.getOrElse(""), obtained.url.toExternalForm)
                        assertEquals(t, obtained.mimeType)
                        assertEquals(mediaCreate.isPrimary.getOrElse(false), obtained.isPrimary)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("updateMedia") {
        val media       = createMedia().head
        val mediaUpdate = MediaUpdate(
            url = Some(URI.create(s"http://www.mbari.org/${Strings.random(10)}.png").toURL),
            caption = Some(Strings.random(1000)),
            credit = Some(Strings.random(255)),
            mediaType = Some(MediaTypes.IMAGE.name),
            isPrimary = Some(true)
        )
        val attempt     = testWithUserAuth(
            user =>
                runPut(
                    endpoints.updateMediaEndpointImpl,
                    s"http://test.com/v1/media/${media.id.get}",
                    mediaUpdate.stringify,
                    response =>
                        assertEquals(response.code, StatusCode.Ok)
                        val obtained = checkResponse[Media](response.body)
                        assertEquals(mediaUpdate.url.orNull, obtained.url)
                        assertEquals(mediaUpdate.caption, obtained.caption)
                        assertEquals(mediaUpdate.credit, obtained.credit)
                        val t        = Media.resolveMimeType(mediaUpdate.mediaType.getOrElse(""), obtained.url.toExternalForm)
                        assertEquals(t, obtained.mimeType)
                        assertEquals(mediaUpdate.isPrimary.getOrElse(false), obtained.isPrimary)
                    ,
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }

    test("deleteMedia") {
        val media   = createMedia().head
        val attempt = testWithUserAuth(
            user =>
                runDelete(
                    endpoints.deleteMediaEndpointImpl,
                    s"http://test.com/v1/media/${media.id.get}",
                    response => assertEquals(response.code, StatusCode.Ok),
                    jwt = jwtService.login(user.username, password, user.toEntity)
                ),
            password
        )
        attempt match
            case Left(value)  => fail(value.toString)
            case Right(value) => assert(true)
    }
