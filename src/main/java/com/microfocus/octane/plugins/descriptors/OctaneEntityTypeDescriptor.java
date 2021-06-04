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

import com.microfocus.octane.plugins.configuration.*;

public class OctaneEntityTypeDescriptor {

    /***
     * Type name
     */
    private String typeName;

    /***
     * Alias
     */
    private String alias;

    /**
     * Short type key , for example US, F, etc
     */
    private String typeAbbreviation;

    /**
     * User friendly label
     */
    private String label;

    /**
     * color used to color typeAbbreviation in coverage widget
     */
    private String typeColor;

    /**
     * used to build url to navigate to entity
     */
    private String nameForNavigation;

    /**
     * name of test tab, used to build url of tests for specific entity
     */
    private String testTabName;

    /**
     * Reference field of test to specific entity, used to build coverage query
     */
    private String testReferenceField;

    /**
     * Name of the field for indirect test covering
     */
    private String indirectCoveringTestsField;

    private static final String TESTS_URL_FOR_DEFECTS = "&configuration={\"tabName\":\"relationships\",\"relation_name\":\"test_to_work_items-gherkin_test-scenario_test-test_automated-test_manual-test_suite-target\"}";

    public OctaneEntityTypeDescriptor(String typeName, String alias, String typeAbbreviation, String label, String typeColor, String nameForNavigation, String testTabName, String testReferenceField, String indirectCoveringTestsField) {
        this.typeName = typeName;
        this.alias = alias;
        this.label = label;
        this.typeAbbreviation = typeAbbreviation;
        this.typeColor = typeColor;
        this.nameForNavigation = nameForNavigation;
        this.testTabName = testTabName;
        this.testReferenceField = testReferenceField;
        this.indirectCoveringTestsField = indirectCoveringTestsField;
    }

    public String getTypeAbbreviation() {
        return typeAbbreviation;
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

    public String buildTestTabEntityUrl(SpaceConfiguration sc, long workspaceId, String entityId, String subtype) {
        VersionEntity versionEntity = OctaneRestManager.getOctaneServerVersion(sc);
        OctaneServerVersion octaneServerVersion = new OctaneServerVersion(versionEntity.getVersion());

        if (("defect").equals(subtype) && octaneServerVersion.isLessThan(new OctaneServerVersion(PluginConstants.GUNSNROSES_PUSH2))) {
            return buildEntityUrl(sc.getLocationParts().getBaseUrl(), sc.getLocationParts().getSpaceId(), workspaceId, entityId) + TESTS_URL_FOR_DEFECTS;
        } else {
            return buildEntityUrl(sc.getLocationParts().getBaseUrl(), sc.getLocationParts().getSpaceId(), workspaceId, entityId) + "&tabName=" + getTestTabName();
        }
    }

    public String getTestReferenceField() {
        return testReferenceField;
    }

    public String getLabel() {
        return label;
    }


    public String getAlias() {
        return alias;
    }

    public String getIndirectCoveringTestsField() {
        return indirectCoveringTestsField;
    }
}
