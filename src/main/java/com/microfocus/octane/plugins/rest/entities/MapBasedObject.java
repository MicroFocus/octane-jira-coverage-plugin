/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
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
 *
 */


package com.microfocus.octane.plugins.rest.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by berkovir on 28/05/2015.
 */
public class MapBasedObject {
    private Map<String, Object> fields = new HashMap<String, Object>();

    public void put(String fieldName, Object fieldValue) {
        fields.put(fieldName, fieldValue);
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Object get(String fieldName) {
        return fields.get(fieldName);
    }

    public String getString(String fieldName) {
        return (String)fields.get(fieldName);
    }


    public void remove(String fieldName){
        fields.remove(fieldName);
    }

    public boolean isFieldSet(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public boolean isFieldSetAndNotEmpty(String fieldName) {
        Object value = fields.get(fieldName);
        if (value == null) {
            return false;
        }

        if (value instanceof String) {
            return !((String) value).isEmpty();
        }

        if (value instanceof Collection) {
            return !((Collection) value).isEmpty();
        }

        return true;
    }

}

