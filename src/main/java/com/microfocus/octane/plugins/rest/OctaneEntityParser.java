/*******************************************************************************
 * Copyright 2017-2023 Open Text.
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.microfocus.octane.plugins.rest;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.microfocus.octane.plugins.configuration.VersionEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntity;
import com.microfocus.octane.plugins.rest.entities.OctaneEntityCollection;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntity;
import com.microfocus.octane.plugins.rest.entities.groups.GroupEntityCollection;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;

public class OctaneEntityParser {

    public static GroupEntityCollection parseGroupCollection(String data) {
        GroupEntityCollection coll = new GroupEntityCollection();
        try {
            JSONObject jsonObj = new JSONObject(data);
            int total = jsonObj.getInt("groupsTotalCount");
            coll.setGroupsTotalCount(total);

            JSONArray entitiesJArr = jsonObj.getJSONArray("groups");
            for (int i = 0; i < entitiesJArr.length(); i++) {

                JSONObject entObj = entitiesJArr.getJSONObject(i);
                GroupEntity entity = parseGroupEntity(entObj);
                coll.getGroups().add(entity);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parseGroupCollection :" + e.getMessage());
        }

        return coll;
    }

    public static OctaneEntityCollection parseCollection(String data) {
        try {
            JSONObject jsonObj = new JSONObject(data);
            return parseCollection(jsonObj);
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parseCollection(string) :" + e.getMessage());
        }

    }

    public static OctaneEntityCollection parseCollection(JSONObject jsonObj) {
        OctaneEntityCollection coll = new OctaneEntityCollection();
        try {

            int total = jsonObj.getInt("total_count");
            coll.setTotalCount(total);

            if (jsonObj.has("exceeds_total_count")) {
                boolean exceedsTotalCount = jsonObj.getBoolean("exceeds_total_count");
                coll.setExceedsTotalCount(exceedsTotalCount);
            }

            JSONArray entitiesJArr = jsonObj.getJSONArray("data");
            for (int i = 0; i < entitiesJArr.length(); i++) {

                JSONObject entObj = entitiesJArr.getJSONObject(i);
                OctaneEntity entity = parseEntity(entObj);

                coll.getData().add(entity);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parseCollection :" + e.getMessage());
        }

        return coll;
    }

    public static OctaneEntity parseEntity(JSONObject entObj) throws JSONException {

        String type = entObj.getString("type");

        OctaneEntity entity = new OctaneEntity(type);
        for (Iterator<String> it = entObj.keys(); it.hasNext(); ) {
            String key = it.next();
            Object value = entObj.get(key);
            if (value instanceof JSONObject) {
                JSONObject jObj = (JSONObject) value;
                if (jObj.has("type")) {
                    OctaneEntity valueEntity = parseEntity(jObj);
                    value = valueEntity;
                } else if (jObj.has("total_count")) {
                    OctaneEntityCollection coll = parseCollection(jObj);
                    value = coll;
                } else {
                    value = jObj.toString();
                }
            }
            entity.put(key, value);
        }
        return entity;
    }

    private static GroupEntity parseGroupEntity(JSONObject entObj) throws JSONException {

        GroupEntity groupEntity = new GroupEntity();
        int count = entObj.getInt("count");
        groupEntity.setCount(count);
        if (!entObj.isNull("value")) {
            Object value = null;
            JSONObject jsonObject = entObj.optJSONObject("value");
            if (jsonObject != null) {
                value = parseEntity(jsonObject);
            } else {
                value = entObj.optString("value");
            }

            groupEntity.setValue(value);
        }

        return groupEntity;
    }

    public static VersionEntity parseServerVersion(String serverVersion) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(serverVersion, VersionEntity.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse ValueEdge server version:" + e.getMessage());
        }
    }
}
