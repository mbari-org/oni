/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.etc.circe

import io.circe
import io.circe.*
import io.circe.generic.semiauto.*
import org.mbari.oni.domain.*
import org.mbari.oni.util.HexUtil

import java.net.{URI, URL}
import scala.util.Try

object CirceCodecs:

    given byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]]:
        final def apply(xs: Array[Byte]): Json =
            Json.fromString(HexUtil.toHex(xs))
    given byteArrayDecoder: Decoder[Array[Byte]] = Decoder
        .decodeString
        .emapTry(str => Try(HexUtil.fromHex(str)))

    given urlDecoder: Decoder[URL] = Decoder
        .decodeString
        .emapTry(str => Try(URI.create(str).toURL))
    given urlEncoder: Encoder[URL] = Encoder
        .encodeString
        .contramap(_.toString)

    // --- Error Responses ---
    given Decoder[ErrorMsg] = deriveDecoder
    given Encoder[ErrorMsg] = deriveEncoder

    given Decoder[BadRequest] = deriveDecoder
    given Encoder[BadRequest] = deriveEncoder

    given Decoder[StatusMsg] = deriveDecoder
    given Encoder[StatusMsg] = deriveEncoder

    given Decoder[NotFound] = deriveDecoder
    given Encoder[NotFound] = deriveEncoder

    given Decoder[ServerError] = deriveDecoder
    given Encoder[ServerError] = deriveEncoder

    given Decoder[Unauthorized] = deriveDecoder
    given Encoder[Unauthorized] = deriveEncoder

    // --- Domain Objects ---
    given Decoder[AuthorizationSC] = deriveDecoder
    given Encoder[AuthorizationSC] = deriveEncoder

    given Decoder[Concept] = deriveDecoder
    given Encoder[Concept] = deriveEncoder

    given Decoder[ConceptCreate] = deriveDecoder
    given Encoder[ConceptCreate] = deriveEncoder

    given Decoder[ConceptDelete] = deriveDecoder
    given Encoder[ConceptDelete] = deriveEncoder

    given Decoder[ConceptNameCreate] = deriveDecoder
    given Encoder[ConceptNameCreate] = deriveEncoder

    given Decoder[ConceptNameUpdate] = deriveDecoder
    given Encoder[ConceptNameUpdate] = deriveEncoder

    given Decoder[ConceptUpdate] = deriveDecoder
    given Encoder[ConceptUpdate] = deriveEncoder

    given Decoder[Count] = deriveDecoder
    given Encoder[Count] = deriveEncoder

    given Decoder[Page[Seq[String]]] = deriveDecoder
    given Encoder[Page[Seq[String]]] = deriveEncoder

    given Decoder[Rank] = deriveDecoder
    given Encoder[Rank] = deriveEncoder

    given Decoder[ReferenceQuery] = deriveDecoder
    given Encoder[ReferenceQuery] = deriveEncoder

    given Decoder[ExtendedHistory] = deriveDecoder
    given Encoder[ExtendedHistory] = deriveEncoder

    given page2Decoder: Decoder[Page[Seq[ExtendedHistory]]] = deriveDecoder
    given page2Encoder: Encoder[Page[Seq[ExtendedHistory]]] = deriveEncoder

    given Decoder[ExtendedLink] = deriveDecoder
    given Encoder[ExtendedLink] = deriveEncoder

    given page3Decoder: Decoder[Page[Seq[ExtendedLink]]] = deriveDecoder
    given page3Encoder: Encoder[Page[Seq[ExtendedLink]]] = deriveEncoder

    given Decoder[Link] = deriveDecoder
    given Encoder[Link] = deriveEncoder

    given Decoder[LinkCreate] = deriveDecoder
    given Encoder[LinkCreate] = deriveEncoder

    given Decoder[LinkRenameToConceptRequest] = deriveDecoder
    given Encoder[LinkRenameToConceptRequest] = deriveEncoder

    given Decoder[LinkRenameToConceptResponse] = deriveDecoder
    given Encoder[LinkRenameToConceptResponse] = deriveEncoder

    given Decoder[LinkUpdate] = deriveDecoder
    given Encoder[LinkUpdate] = deriveEncoder

    given Decoder[Media] = deriveDecoder
    given Encoder[Media] = deriveEncoder

    given Decoder[MediaCreate] = deriveDecoder
    given Encoder[MediaCreate] = deriveEncoder

    given Decoder[MediaUpdate] = deriveDecoder
    given Encoder[MediaUpdate] = deriveEncoder

    given Decoder[ConceptMetadata] = deriveDecoder
    given Encoder[ConceptMetadata] = deriveEncoder

    given Decoder[HealthStatus] = deriveDecoder
    given Encoder[HealthStatus] = deriveEncoder

    given Decoder[PrefNode] = deriveDecoder
    given Encoder[PrefNode] = deriveEncoder

    given Decoder[PrefNodeUpdate] = deriveDecoder
    given Encoder[PrefNodeUpdate] = deriveEncoder

    given Decoder[RawConcept] = deriveDecoder
    given Encoder[RawConcept] = deriveEncoder

    given Decoder[RawConceptMetadata] = deriveDecoder
    given Encoder[RawConceptMetadata] = deriveEncoder

    given Decoder[RawConceptName] = deriveDecoder
    given Encoder[RawConceptName] = deriveEncoder

    given Decoder[RawLink] = deriveDecoder
    given Encoder[RawLink] = deriveEncoder

    given Decoder[RawMedia] = deriveDecoder
    given Encoder[RawMedia] = deriveEncoder

    given Decoder[Reference] = deriveDecoder
    given Encoder[Reference] = deriveEncoder

    given page4Decoder: Decoder[Page[Seq[Reference]]] = deriveDecoder
    given page4Encoder: Encoder[Page[Seq[Reference]]] = deriveEncoder

    given Decoder[ReferenceUpdate] = deriveDecoder
    given Encoder[ReferenceUpdate] = deriveEncoder

    given Decoder[SerdeConcept] = deriveDecoder
    given Encoder[SerdeConcept] = deriveEncoder

    given Decoder[SimpleConcept] = deriveDecoder
    given Encoder[SimpleConcept] = deriveEncoder

    given Decoder[UserAccount] = deriveDecoder
    given Encoder[UserAccount] = deriveEncoder

    given Decoder[UserAccountCreate] = deriveDecoder
    given Encoder[UserAccountCreate] = deriveEncoder

    given Decoder[UserAccountUpdate] = deriveDecoder
    given Encoder[UserAccountUpdate] = deriveEncoder

    val CustomPrinter: Printer = Printer(
        dropNullValues = true,
        indent = ""
    )

    /**
     * Convert a circe Json object to a JSON string
     *
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension (json: Json) def stringify: String = CustomPrinter.print(json)

    /**
     * Convert an object to a JSON string
     *
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension [T: Encoder](value: T)
        def stringify: String = Encoder[T]
            .apply(value)
            .deepDropNullValues
            .stringify

    extension [T: Decoder](jsonString: String) def toJson: Either[ParsingFailure, Json] = parser.parse(jsonString);

    extension (jsonString: String)
        def reify[T: Decoder]: Either[Error, T] =
            for
                json   <- jsonString.toJson
                result <- Decoder[T].apply(json.hcursor)
            yield result
