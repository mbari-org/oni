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

package org.mbari.oni.domain

import scala.reflect.ClassTag



final case class Sort(field: String, direction: Sort.Direction = Sort.Direction.Ascending):

    def sort[T: ClassTag](seq: Seq[T]): Seq[T] =
        val clazz = summon[ClassTag[T]].runtimeClass
        scala.util.Try(clazz.getDeclaredField(field)) match
            case scala.util.Failure(_) => seq
            case scala.util.Success(f) =>
                f.setAccessible(true)
                // Helper function to unwrap Option values and handle nulls
                // This allows sorting by both value and optional fields.
                def unwrap(v: AnyRef): Option[Comparable[Any]] =
                    v match
                        case null        => None
                        case Some(inner) => Option(inner.asInstanceOf[AnyRef]).map(_.asInstanceOf[Comparable[Any]])
                        case None        => None
                        case plain       => Some(plain.asInstanceOf[Comparable[Any]])
                val sorted                                     = seq.sortWith { (a, b) =>
                    (unwrap(f.get(a)), unwrap(f.get(b))) match
                        case (None, _)            => true
                        case (_, None)            => false
                        case (Some(va), Some(vb)) => va.compareTo(vb) < 0
                }
                direction match
                    case Sort.Direction.Ascending  => sorted
                    case Sort.Direction.Descending => sorted.reverse

object Sort:

    enum Direction:
        case Ascending  extends Direction
        case Descending extends Direction

    /**
     * Parse a sort string in the format "field,direction" where direction is optional and can be "asc" or "desc". If
     * direction is not provided, it defaults to ascending. The field is the name of the field to sort by. The function
     * returns an Option[Sort] which will be None if the input string is not in the correct format or if the direction
     * is invalid. 
     * 
     * Examples of valid input strings:
     *   - "processedTimestamp,asc" => Sort("processedTimestamp", SortDirection.Ascending)
     *   - "concept,desc" => Sort("concept", SortDirection.Descending)
     *   - "concept" => Sort("concept", SortDirection.Ascending)
     *
     * @param s
     * @return
     */
    def fromString(s: String): Option[Sort] =
        val parts = s.split(",").map(_.trim)
        if parts.nonEmpty then
            val field         = parts.head
            val direction     = if parts.length > 1 then parts(1).toLowerCase else "asc"
            val sortDirection = direction match
                case "asc"  => Sort.Direction.Ascending
                case "desc" => Sort.Direction.Descending
                case _      => return None
            Some(Sort(field, sortDirection))
        else None
