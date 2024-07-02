/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain

import java.net.URI

/**
 * Query DTO for references. One of the fields, doi or citation, must be set.
 * @param doi
 *   A complete DOI (optional)
 * @param citation
 *   A partial citation (optional)
 */
case class ReferenceQuery(doi: Option[URI] = None, citation: Option[String] = None)
