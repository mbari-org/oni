/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.services;

import jakarta.persistence.EntityManagerFactory;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

public class VarsUserPreferencesFactory implements PreferencesFactory {

    final EntityManagerFactory entityManagerFactory;

    public VarsUserPreferencesFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Preferences systemRoot() {
        return new VarsUserPreferences(entityManagerFactory, null, "");
    }

    @Override
    public Preferences userRoot() {
        throw new UnsupportedOperationException("Not supported on the server");
    }

    public Preferences userRoot(String userName) {
        return systemRoot().node(userName);
    }

}
