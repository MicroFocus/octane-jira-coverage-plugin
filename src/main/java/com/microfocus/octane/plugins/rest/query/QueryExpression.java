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

/**
 * Represent expression value of query, for example "=5" or ">5"
 */
public class QueryExpression {

    private ComparisonOperator operator = ComparisonOperator.Equal;
    private Object value;

    public QueryExpression() {

    }

    public QueryExpression(Object value) {
        this.value = value;
    }

    public QueryExpression(Object value, ComparisonOperator op) {
        this.value = value;
        this.operator = op;
    }

    public Object getValue() {
        return value;
    }


    public ComparisonOperator getOperator() {
        return operator;
    }


}
