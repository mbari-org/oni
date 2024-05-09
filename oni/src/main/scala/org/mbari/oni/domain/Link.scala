/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import org.mbari.oni.domain.ILink

/**
 * @author
 *   Brian Schlining
 * @since 2016-11-17T16:23:00
 */
case class Link(linkName: String, toConcept: String, linkValue: String)

object Link:
    def from(link: ILink): Link =
        Link(link.getLinkName, link.getToConcept, link.getLinkValue)
