/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.oni.domain;

import java.net.URL;

public record MediaNode(URL url, String caption, String credit, String mimeType, Boolean isPrimary) {
}
