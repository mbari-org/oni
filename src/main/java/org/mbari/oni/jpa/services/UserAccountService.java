/*
 * @(#)UserAccountService.java   2009.10.01 at 04:47:44 PDT
 *
 * Copyright 2009 MBARI
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbari.oni.jpa.services;

import java.util.*;

import jakarta.inject.Inject;
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

    @Inject
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
