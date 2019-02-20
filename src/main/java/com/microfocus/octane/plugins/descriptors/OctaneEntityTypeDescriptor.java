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

package com.microfocus.octane.plugins.descriptors;

public class OctaneEntityTypeDescriptor {

    private String aggregatedType;
    private String typeName;
    private String typeKey;
    private String typeColor;
    private String nameForNavigation;
    private String testTabName;
    private String testReferenceField;
    private boolean hierarchicalEntity;

    public OctaneEntityTypeDescriptor(String typeName, String typeKey, String typeColor, String nameForNavigation, String testTabName, String testReferenceField, boolean hierarchicalEntity) {
        this.typeName = typeName;
        this.hierarchicalEntity = hierarchicalEntity;
        this.typeKey = typeKey;
        this.typeColor = typeColor;
        this.nameForNavigation = nameForNavigation;
        this.testTabName = testTabName;
        this.testReferenceField = testReferenceField;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeColor() {
        return typeColor;
    }

    public String getNameForNavigation() {
        return nameForNavigation;
    }

    public String getTestTabName() {
        return testTabName;
    }

    public String buildEntityUrl(String baseUrl, long spaceId, long workspaceId, String entityId) {
        String octaneEntityUrl = String.format("%s/ui/?p=%s/%s#/entity-navigation?entityType=%s&id=%s",
                baseUrl, spaceId, workspaceId,
                this.getNameForNavigation(), entityId);
        return octaneEntityUrl;
    }

    public String buildTestTabEntityUrl(String baseUrl, long spaceId, long workspaceId, String entityId) {
        return buildEntityUrl(baseUrl, spaceId, workspaceId, entityId) + "&tabName=" + getTestTabName();
    }


    public String getTestReferenceField() {
        return testReferenceField;
    }

    public boolean isHierarchicalEntity() {
        return hierarchicalEntity;
    }

}
