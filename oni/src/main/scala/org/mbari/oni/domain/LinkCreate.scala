/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

case class LinkCreate(
    concept: String,
    linkName: String,
    toConcept: String = ILink.VALUE_SELF,
    linkValue: String = ILink.VALUE_NIL
):

    /**
     * Convert this object to a Link. You can convert a link to a LinkRealizationEntity or LinkTemplateEntity
     * @return
     *   A Link representaiton of this object
     */
    def toLink: Link =
        Link(linkName, toConcept, linkValue)
