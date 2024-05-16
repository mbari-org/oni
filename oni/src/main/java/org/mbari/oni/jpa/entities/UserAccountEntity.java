/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2024
 *
 * oni code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential. 
 */

package org.mbari.oni.jpa.entities;

import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.mbari.oni.domain.UserAccountRoles;
import org.mbari.oni.jpa.KeyNullifier;
import org.mbari.oni.jpa.TransactionLogger;
import org.mbari.oni.jpa.IPersistentObject;

/**
 * Class description
 *
 *
 * @version        $date$, 2009.11.10 at 11:55:32 PST
 * @author         Brian Schlining [brian@mbari.org]
 */
@Entity(name = "UserAccount")
@Table(name = "UserAccount")
@EntityListeners({ TransactionLogger.class, KeyNullifier.class })
@NamedQueries( {

    @NamedQuery(name = "UserAccount.findById", query = "SELECT v FROM UserAccount v WHERE v.id = :id") ,
    @NamedQuery(name = "UserAccount.findByUserName",
                query = "SELECT v FROM UserAccount v WHERE v.userName = :userName") ,
    @NamedQuery(name = "UserAccount.findByFirstName",
                query = "SELECT c FROM UserAccount c WHERE c.firstName = :firstName") ,
    @NamedQuery(name = "UserAccount.findByLastName",
                query = "SELECT c FROM UserAccount c WHERE c.lastName = :lastName") ,
    @NamedQuery(name = "UserAccount.findByAffiliation",
                query = "SELECT c FROM UserAccount c WHERE c.affiliation LIKE :affiliation") ,
    @NamedQuery(name = "UserAccount.findByRole", query = "SELECT c FROM UserAccount c WHERE c.role LIKE :role") ,
    @NamedQuery(name = "UserAccount.findAll", query = "SELECT c FROM UserAccount c")

})
public class UserAccountEntity implements Serializable, IPersistentObject {

    @Column(name = "Affiliation", length = 512)
    String affiliation;

    @Column(name = "Email", length = 50)
    String email;

    @Column(name = "FirstName", length = 50)
    String firstName;

    @Id
    @Column(
        name = "id",
        nullable = false,
        updatable = false
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "UserAccount_Gen")
    @TableGenerator(
        name = "UserAccount_Gen",
        table = "UniqueID",
        pkColumnName = "TableName",
        valueColumnName = "NextID",
        pkColumnValue = "UserName",
        allocationSize = 1
    )
    Long id;

    @Column(name = "LastName", length = 50)
    String lastName;

    @Column(
        name = "Password",
        nullable = false,
        length = 50
    )
    String encryptedPassword;
    
    @Column(
        name = "Role",
        nullable = false,
        length = 10
    )
    String role;

    /** Optimistic lock to prevent concurrent overwrites */
    @Version
    @Column(name = "LAST_UPDATED_TIME")
    private Timestamp updatedTime;
    @Column(
        name = "UserName",
        nullable = false,
        unique = true,
        length = 50
    )
    String userName;

    public boolean authenticate(String unencryptedPassword) {
        return (new BasicPasswordEncryptor()).checkPassword(unencryptedPassword, encryptedPassword);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final UserAccountEntity other = (UserAccountEntity) obj;
        if ((this.userName == null) ? (other.userName != null) : !this.userName.equals(other.userName)) {
            return false;
        }

        return true;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public Long getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public String getRole() {
        return role;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + ((this.userName != null) ? this.userName.hashCode() : 0);

        return hash;
    }

    public boolean isAdministrator() {
        return UserAccountRoles.ADMINISTRATOR.getRoleName().equals(role);
    }

    public boolean isMaintainer() {
        return UserAccountRoles.MAINTENANCE.getRoleName().equals(role);
    }

    public boolean isReadOnly() {
        return UserAccountRoles.READONLY.getRoleName().equals(role);
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public void setId(Long id) {
    	this.id = id;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setPassword(String password) {
        this.encryptedPassword = (new BasicPasswordEncryptor()).encryptPassword(password);
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" ([id=").append(getId()).append("] ");
        sb.append("userName=").append(userName).append(")");
        return sb.toString();
    }
    
    public Object getPrimaryKey() {
    	return getId();
    }
}
