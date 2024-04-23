package org.mbari.oni.domain;

import java.net.URL;

public record MediaNode(URL url, String caption, String credit, String mimeType, Boolean isPrimary) {
}
