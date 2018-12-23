/*
 *     Copyright 2018 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.microfocus.octane.plugins.rest.query;

import java.util.Collection;

public class InQueryPhrase implements QueryPhrase {

    private String fieldName;
    private Collection<String> values;

    public InQueryPhrase(String fieldName, Collection<String> values) {
        this.fieldName = fieldName;
        this.values = values;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Collection<String> getValues() {
        return values;
    }
}
