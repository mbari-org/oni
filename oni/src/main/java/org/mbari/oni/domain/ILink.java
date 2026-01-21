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
 * Inteface for VARS link style classes (Association, LinkTemplate, LinkRealization)
 */
public interface ILink {

    String PROP_LINKNAME = "linkName";
    String PROP_LINKVALUE = "linkValue";
    String PROP_TOCONCEPT = "toConcept";
    String VALUE_NIL = "nil";
    String VALUE_SELF = "self";
    /**
     * Delimiter for String representations
     */
    String DELIMITER = " | ";
    String DELIMITER_REGEXP = " \\| ";

    String getFromConcept();

    String getLinkName();

    void setLinkName(String linkName);

    String getToConcept();

    void setToConcept(String toConcept);

    String getLinkValue();

    void setLinkValue(String linkValue);

    /**
     * @return A delimited representation of the data contents in the form:
     * [fromConcept]DELIMITER[linkName]DELIMITER[toConcept]DELIMITER[linkValue]
     */
    String stringValue();

}
