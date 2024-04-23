package org.mbari.oni.jpa;


import jakarta.persistence.PostRemove;
import org.mbari.oni.etc.jdk.Logging;

/**
 * An EntityListener that sets the primary key of an Entity object to null after it's been
 * deleted. This is handy since a null key indicates the object is not a persisted object.
 */
public class KeyNullifier {

    public static final Logging log = new Logging(KeyNullifier.class);

    @PostRemove
    public void nullifyKey(Object object) {
        if (object instanceof IPersistentObject entity) {
            entity.setId(null);
        }
    }
}
