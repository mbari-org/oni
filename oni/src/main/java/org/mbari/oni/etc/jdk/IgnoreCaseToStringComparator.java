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

package org.mbari.oni.etc.jdk;

import java.util.Comparator;

public class IgnoreCaseToStringComparator<T> implements Comparator<T> {

    @Override
    public int compare(T s1, T s2) {
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        } else if (s2 == null) {
            return 1;
        } else {
            return s1.toString().compareToIgnoreCase(s2.toString());
        }
    }
}
