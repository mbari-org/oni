package org.mbari.oni.domain;

import java.util.Set;

public record PhylogenyNode(String name,
                            String rank,
                            PhylogenyNode parent,
                            Set<PhylogenyNode> children) {
}
