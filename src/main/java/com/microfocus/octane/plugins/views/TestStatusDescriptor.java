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

package com.microfocus.octane.plugins.views;

public class TestStatusDescriptor {

    private String logicalName;
    private String title;
    private String color;
    private String key;
    private int order;

    public TestStatusDescriptor(String logicalName, String key, String title, String color, int order) {
        this.logicalName = logicalName;
        this.title = title;
        this.color = color;
        this.order = order;
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getColor() {
        return color;
    }

    public int getOrder() {
        return order;
    }

    public String getKey() {
        return key;
    }
}
