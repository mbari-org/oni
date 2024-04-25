/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.services;

import java.util.*;

import jakarta.persistence.EntityManager;
import org.mbari.oni.jpa.entities.UserAccountEntity;


/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 19, 2009
 * Time: 3:09:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class UserAccountService extends Service {

    public UserAccountService(EntityManager entityManager) {
        super(entityManager);
    }


    public Collection<UserAccountEntity> findAll() {
        return findByNamedQuery("UserAccount.findAll");
    }

    public Collection<UserAccountEntity> findAll(int limit, int offset) {
        return findByNamedQuery("UserAccount.findAll", limit, offset);
    }

    public Collection<UserAccountEntity> findAllByFirstName(String firstName) {
        return findByNamedQuery("UserAccount.findByFirstName", Map.of("firstName", firstName));
    }

    public Collection<UserAccountEntity> findAllByLastName(String lastName) {
        return findByNamedQuery("UserAccount.findByLastName", Map.of("lastName", lastName));
    }

    public Collection<UserAccountEntity> findAllByRole(String role) {
        return findByNamedQuery("UserAccount.findByRole", Map.of("role", role));
    }

    /**
     * Search for the matching username
     * @param userName The username to search for
     * @return the match, or null if no match is found
     */
    public Optional<UserAccountEntity> findByUserName(String userName) {
        List<UserAccountEntity> accounts = findByNamedQuery("UserAccount.findByUserName", Map.of("userName", userName));
        return accounts.stream().findFirst();
    }
}
