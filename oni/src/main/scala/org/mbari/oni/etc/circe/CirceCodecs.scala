/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.etc.circe

import io.circe.*
import io.circe.generic.semiauto.*
import scala.util.Try
import java.net.URL
import org.mbari.oni.util.HexUtil
import java.net.URI
import org.mbari.oni.domain.*

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
    given Decoder[Authorization] = deriveDecoder
    given Encoder[Authorization] = deriveEncoder

    given Decoder[Concept] = deriveDecoder
    given Encoder[Concept] = deriveEncoder
    
    given Decoder[ExtendedHistory] = deriveDecoder
    given Encoder[ExtendedHistory] = deriveEncoder

    given Decoder[Link] = deriveDecoder
    given Encoder[Link] = deriveEncoder

    given Decoder[Media] = deriveDecoder
    given Encoder[Media] = deriveEncoder

    given Decoder[ConceptMetadata] = deriveDecoder
    given Encoder[ConceptMetadata] = deriveEncoder

    given Decoder[HealthStatus] = deriveDecoder
    given Encoder[HealthStatus] = deriveEncoder

    given Decoder[PrefNode] = deriveDecoder
    given Encoder[PrefNode] = deriveEncoder

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

    given Decoder[SerdeConcept] = deriveDecoder
    given Encoder[SerdeConcept] = deriveEncoder

    given Decoder[SimpleConcept] = deriveDecoder
    given Encoder[SimpleConcept] = deriveEncoder

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
