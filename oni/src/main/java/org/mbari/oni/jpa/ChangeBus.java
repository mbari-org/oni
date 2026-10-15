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

import org.mbari.oni.etc.jdk.Logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A synchronous, in-process publish/subscribe bus for {@link ChangeEvent}s. Events are only published after the
 * change has been committed to the database, so subscribers (e.g. caches) never observe uncommitted data. A failing
 * subscriber never affects other subscribers or the code that made the change.
 */
public final class ChangeBus {

    private static final Logging log = new Logging(ChangeBus.class);
    private static final List<Consumer<ChangeEvent>> subscribers = new CopyOnWriteArrayList<>();

    private ChangeBus() {}

    /**
     * @return a Runnable that unsubscribes the subscriber when run
     */
    public static Runnable subscribe(Consumer<ChangeEvent> subscriber) {
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    public static void publish(ChangeEvent event) {
        for (var subscriber : subscribers) {
            try {
                subscriber.accept(event);
            }
            catch (RuntimeException e) {
                log.atWarn().withCause(e).log("A ChangeBus subscriber failed handling " + event);
            }
        }
    }

    public static void publish(ChangeEvent.Action action, ChangeEvent.DataType dataType, Object id) {
        publish(new ChangeEvent(action, dataType, id));
    }
}
