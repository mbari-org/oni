/*
 * Copyright 2024 Monterey Bay Aquarium Research Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbari.oni.jpa.repositories;

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
public class UserAccountRepository extends Repository {

    public UserAccountRepository(EntityManager entityManager) {
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
