/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jdbc;

import org.mbari.oni.etc.jdk.Logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MutableConcept {
    private Long id;
    private MutableConcept parent;
    private String rank;
    private List<CName> names = new ArrayList<>();
    private List<MutableConcept> children = new ArrayList<>();
    private static final Logging log = new Logging(MutableConcept.class.getName());

    public MutableConcept() {
    }

    public MutableConcept(Long id, String rank, List<CName> names, MutableConcept parent, List<MutableConcept> children) {
        this.id = id;
        this.parent = parent;
        this.rank = rank;
        this.names = names;
        this.children = children;
    }

    public MutableConcept(Long id) {
        this.id = id;
    }

    public MutableConcept(Long id, String rank, List<CName> names, List<MutableConcept> children) {
        this.id = id;
        this.rank = rank;
        this.names = names;
        this.children = children;
    }

    public MutableConcept root() {
        if (parent == null) {
            return this;
        }
        return parent.root();
    }

    public MutableConcept copyUp() {
        return copyUp(List.of());
    }

    public MutableConcept copyUp(List<MutableConcept> newChildren) {
        var mc = new MutableConcept(id, rank, names, newChildren);
        mc.setParent(parent.copyUp(List.of(mc)));
        return mc;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public List<CName> getNames() {
        return names;
    }

    public void setNames(List<CName> names) {
        this.names = names;
    }

    public List<MutableConcept> getChildren() {
        return children;
    }

    public void setChildren(List<MutableConcept> children) {
        this.children = children;
    }

    public MutableConcept getParent() {
        return parent;
    }

    public void setParent(MutableConcept parent) {
        this.parent = parent;
    }

    public String getPrimaryName() {
        return names.stream()
                .filter(n -> n.nameType().equals("primary"))
                .findFirst()
                .map(CName::name)
                .orElse(null);
    }

    /**
     * Convert a list of ConceptRow objects to a tree of MutableConcept objects.
     * @param rows List of ConceptRow objects, basiclly the rows from the database table
     * @return A Pair of the root MutableConcept and a list of all MutableConcepts
     */
    public static TreeData toTree(List<ConceptRow> rows) {
        var nodes = new ArrayList<MutableConcept>();
        for (var row: rows) {

            // --- Find an existing parent MutableConcept or create one as needed.
            // This will be empty if it's the root node
            Optional<MutableConcept> parentOpt = Optional.empty();
            if (row.parentId() == null) {
                log.atInfo().log("Found root node: " + row);
            }
            else {
                parentOpt = Optional.of(nodes.stream()
                        .filter(n -> n.getId() != null && n.getId().equals(row.parentId()))
                        .findFirst()
                        .orElseGet(() -> {
                            var mc = new MutableConcept(row.parentId());
                            nodes.add(mc);
                            return mc;
                        }));
            }

            // --- Find the existing MutableConcept or create a new one
            MutableConcept self = nodes.stream()
                    .filter(n -> n.getId() != null && n.getId().equals(row.id()))
                    .findFirst()
                    .orElseGet(() -> {
                        var newMc = new MutableConcept(row.id());
                        nodes.add(newMc);
                        return newMc;
                    });

            parentOpt.ifPresent(parent -> {
                self.setParent(parent);
                parent.getChildren().add(self);
            });

            var cn = new CName(row.name(), row.nameType());
            self.setRank(row.rank().orElse(null));
            self.getNames().add(cn);
        }

        var root = nodes.stream()
                .filter(n -> n.getParent() == null)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No root node found"));
        return new TreeData(root, nodes);
    }

}
