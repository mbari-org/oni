package org.mbari.oni.jpa.services;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

@Singleton
public class VarsUserPreferencesFactory implements PreferencesFactory {

    @Inject
    EntityManagerFactory entityManagerFactory;

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
