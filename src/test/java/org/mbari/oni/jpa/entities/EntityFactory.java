package org.mbari.oni.jpa.entities;

import org.mbari.oni.domain.ConceptNameTypes;
import org.mbari.oni.domain.MediaTypes;
import org.mbari.oni.etc.jdk.Strings;

import java.util.Random;

public class EntityFactory {

    private static final Random random = new Random();

    public static void buildTree(int depth, int breadth) {
        buildTree(null, depth, breadth);
    }

    public static void buildTree(ConceptEntity parent, int depth, int breadth) {
        var e =  new ConceptEntity();
        if (parent != null) {
            parent.addChildConcept(e);
        }

        // Add random names
        var numNames = random.nextInt(breadth - 1) + 1;
        for (int i = 0; i < numNames; i++) {
            var name = newConceptNameEntity();
            if (i == 0) {
                name.setNameType(ConceptNameTypes.PRIMARY.getName());
            }
            else {
                switch (random.nextInt(4)) {
                    case 0:
                        name.setNameType(ConceptNameTypes.COMMON.getName());
                        break;
                    case 1:
                        name.setNameType(ConceptNameTypes.SYNONYM.getName());
                        break;
                    case 2:
                        name.setNameType(ConceptNameTypes.ALTERNATE.getName());
                        break;
                    case 3:
                        name.setNameType(ConceptNameTypes.FORMER.getName());
                        break;
                }
            }
            e.addConceptName(newConceptNameEntity());
        }

        var m = e.getConceptMetadata();
        // Add LinkTemplates
        var numLinkTemplates = random.nextInt(breadth);
        for (int i = 0; i < numLinkTemplates; i++) {
            m.addLinkTemplate(newLinkTemplateEntity());
        }

        // Add LinkRealizations
        var numLinkRealizations = random.nextInt(breadth);
        for (int i = 0; i < numLinkRealizations; i++) {
            m.addLinkRealization(linkRealizationEntity());
        }

        // Add Media
        var numMedia = random.nextInt(breadth);
        for (int i = 0; i < numMedia; i++) {
            m.addMedia(newMediaEntity());
        }

        // Add child concepts
        if (depth > 0) {
            var numChildren = random.nextInt(breadth);
            for (int i = 0; i < numChildren; i++) {
                buildTree(e, depth - 1, breadth);
            }
        }
    }

    public static ConceptNameEntity newConceptNameEntity() {
        var e = new ConceptNameEntity();
        e.setName(Strings.randomString(20));
        e.setAuthor(Strings.randomString(20));
        return e;
    }


    public static MediaEntity newMediaEntity() {
        var e = new MediaEntity();
        var s = "http://www.mbari.org/path/to/media/" + Strings.randomString(30) + ".png";
        e.setUrl(s);
        e.setCaption(Strings.randomString(255));
        e.setCredit(Strings.randomString(255));
        e.setType(MediaTypes.IMAGE.getType());
        return e;
    }

    public static LinkTemplateEntity newLinkTemplateEntity() {
        var e = new LinkTemplateEntity();
        e.setLinkName(Strings.randomString(20));
        e.setLinkValue(Strings.randomString(255));
        e.setToConcept(Strings.randomString(20));
        return e;
    }

    public static LinkRealizationEntity linkRealizationEntity() {
        var e =  new LinkRealizationEntity();
        e.setLinkName(Strings.randomString(20));
        e.setLinkValue(Strings.randomString(255));
        e.setToConcept(Strings.randomString(20));
        return e;
    }
}
