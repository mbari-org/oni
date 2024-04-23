/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mbari.oni.jpa;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.mbari.oni.etc.jdk.Logging;


/**
 *
 * @author brian
 */
public class TransactionLogger {

    private static final Logging log = new Logging(TransactionLogger.class);

    public enum TransactionType {
        PERSIST,
        REMOVE,
        MERGE
    }

    public TransactionLogger() {
    }

    @PostLoad
    public void logLoad(Object object) {
        if (log.logger().isLoggable(System.Logger.Level.DEBUG)) {
            log.atDebug().log("Loaded " + object + " into persistent context");
        }
    }

    @PrePersist
    public void logPersist(Object object) {
        logTransaction(object, TransactionType.PERSIST);
    }

    @PreRemove
    public void logRemove(Object object) {
        logTransaction(object, TransactionType.REMOVE);
    }

    @PreUpdate
    public void logUpdate(Object object) {
        logTransaction(object, TransactionType.MERGE);
    }

    private void logTransaction(Object object, TransactionType transactionType) {
        log.atDebug().log(() -> "Performing " + transactionType + " on " + object);
    }
}