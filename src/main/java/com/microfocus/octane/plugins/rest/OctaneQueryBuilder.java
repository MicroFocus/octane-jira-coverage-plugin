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


package com.microfocus.octane.plugins.rest;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by berkovir on 06/12/2016.
 */
public class OctaneQueryBuilder implements Serializable {


    private StringBuilder sb;
    private Integer pageSize;
    private Integer startIndex;
    private List<String> selectedFields;
    private List<String> orderBy;
    private Map<String, String> queryConditions;


    public static OctaneQueryBuilder create() {
        return new OctaneQueryBuilder();
    }

    public OctaneQueryBuilder addQueryConditions(Map<String, String> conditions) {
        if (conditions != null && !conditions.isEmpty()) {
            if (this.queryConditions == null) {
                this.queryConditions = new HashMap<>();
            }
            this.queryConditions.putAll(conditions);
        }
        return this;
    }

    public OctaneQueryBuilder addQueryCondition(String field, String value) {
        if (queryConditions == null) {
            queryConditions = new HashMap<>();
        }
        queryConditions.put(field, value);
        return this;
    }

    public OctaneQueryBuilder addStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
        return this;
    }

    public OctaneQueryBuilder addPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public OctaneQueryBuilder addSelectedFields(String... fieldNames) {
        return addSelectedFields(Arrays.asList(fieldNames));
    }

    public OctaneQueryBuilder addSelectedFields(Collection<String> fieldNames) {
        if (fieldNames != null && !fieldNames.isEmpty()) {
            if (selectedFields == null) {
                selectedFields = new ArrayList<>();
            }
            selectedFields.addAll(fieldNames);
        }
        return this;
    }

    public OctaneQueryBuilder addOrderBy(String... fieldNames) {
        return addOrderBy(Arrays.asList(fieldNames));
    }

    public OctaneQueryBuilder addOrderBy(Collection<String> fieldNames) {
        if (fieldNames != null && !fieldNames.isEmpty()) {
            if (orderBy == null) {
                orderBy = new ArrayList<>();
            }
            orderBy.addAll(fieldNames);
        }
        return this;
    }

    public String build() {
        sb = new StringBuilder();
        buildQuery();

        buildPageSize();
        buildStartIndex();

        buildSelectedFields();
        buildOrderBy();
        return sb.toString();
    }

    private void buildSelectedFields() {
        if (selectedFields != null && !selectedFields.isEmpty()) {
            sb.append("&").append("fields=").append(StringUtils.join(selectedFields, ","));
        }
    }

    private void buildQuery() {
       // query="id='100'"
        if (queryConditions != null && !queryConditions.isEmpty()) {
            String splitter = "";
            sb.append("&").append("query=\"");
            if (queryConditions != null) {
                for (Map.Entry<String, String> entry : queryConditions.entrySet()) {
                    sb.append(splitter);

                    //if value contains spaces (for example A OR B), spaces should be converted to '+',
                    //if value wrapped with ' , vor example 'Not Completed', it should be handled as is
                    String value = entry.getValue();//.startsWith("'") ? entry.getValue() : entry.getValue().replaceAll(" ", "+");
                    sb.append(entry.getKey()).append("='").append(value).append("'");
                    splitter = ";";


                }
            }

            sb.append("\"");
        }
    }

    private void buildStartIndex() {
        if (startIndex != null) {
            sb.append("&").append("offset=").append(startIndex);
        }
    }

    private void buildPageSize() {
        if (pageSize != null) {
            sb.append("&").append("limit=").append(pageSize);
        }
    }

    private void buildOrderBy() {
        if (orderBy != null && !orderBy.isEmpty()) {
            sb.append("&").append("order_by=").append("{").append(StringUtils.join(orderBy, ",")).append("}");
        }
    }


    public Map<String, String> getQueryConditions() {
        return queryConditions;
    }

    public OctaneQueryBuilder clone() {
        OctaneQueryBuilder qb = (OctaneQueryBuilder) SerializationUtils.clone(this);
        return qb;
    }
}
