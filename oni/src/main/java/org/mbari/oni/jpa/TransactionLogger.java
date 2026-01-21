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