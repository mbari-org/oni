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

package org.mbari.oni.domain;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 19, 2009
 * Time: 2:56:30 PM
 * To change this template use File | Settings | File Templates.
 */
public enum UserAccountRoles {

    ADMINISTRATOR("Admin"), MAINTENANCE("Maint"), READONLY("ReadOnly");

    private final String roleName;


    UserAccountRoles(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public String toString() {
        return roleName;
    }
    
    /**
     * Return the role that corresponds to 'roleName'. Useful for matching the
     * value that's stored in the database to the correct role.
     * @param roleName The string name of the role (Admin, Maint or ReadOnly). The serach
     * 		is case insensitive and will match using the roleName or long name of the role
     * @return The matching UserAccountRole or null of the string provided doens't match any
     * 		of the roles
     */
    public static UserAccountRoles getRole(String roleName) {
    	UserAccountRoles[] roles = values();
    	UserAccountRoles matchingRole = null;
    	for(UserAccountRoles role: roles) {
    		if (role.getRoleName().toLowerCase().startsWith(roleName.toLowerCase())) {
    			matchingRole = role;
    			break;
    		}
    	}
    	return matchingRole;
    }


}
