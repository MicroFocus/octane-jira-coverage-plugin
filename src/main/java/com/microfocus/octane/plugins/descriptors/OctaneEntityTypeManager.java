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

package com.microfocus.octane.plugins.descriptors;

import java.util.*;

public class OctaneEntityTypeManager {

    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByName = new HashMap<>();
    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByLabel = new HashMap<>();
    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByAlias = new HashMap<>();
    private static List<AggregateDescriptor> aggregators = new ArrayList<>();

    static {
        //TYPE
        OctaneEntityTypeDescriptor featureType = new OctaneEntityTypeDescriptor("feature", null, "F", "Feature", "#e57828", "work_item", "tests_in_backlog", "covered_content", "work_items_of_last_run");
        OctaneEntityTypeDescriptor storyType = new OctaneEntityTypeDescriptor("story", null, "US", "User Story", "#ffb000", "work_item", "tests_in_backlog", "covered_content", "work_items_of_last_run");
        OctaneEntityTypeDescriptor defectType = new OctaneEntityTypeDescriptor("defect", null, "D", "Defect", "#b21646", "work_item", "tests_in_backlog", "covered_content", "work_items_of_last_run");

        OctaneEntityTypeDescriptor requirementType = new OctaneEntityTypeDescriptor("requirement_document", null, "RD", "Requirement", "#0b8eac", "requirement", "tests_in_requirement", "covered_requirement", null);
        OctaneEntityTypeDescriptor applicationModuleType = new OctaneEntityTypeDescriptor("product_area", "application_module", "AM", "Application Module", "#43e4ff", "product_area", "tests-in-pa", "product_areas", null);

        Arrays.asList(applicationModuleType, featureType, storyType, requirementType, defectType).forEach(descriptor -> {
            typeDescriptorsByName.put(descriptor.getTypeName(), descriptor);
            typeDescriptorsByLabel.put(descriptor.getLabel(), descriptor);
            if (descriptor.getAlias() != null) {
                typeDescriptorsByAlias.put(descriptor.getAlias(), descriptor);
            }
        });

        aggregators.add(new AggregateDescriptor("work_items", Arrays.asList(featureType, storyType, defectType)));
        aggregators.add(new AggregateDescriptor("application_modules", Arrays.asList(applicationModuleType)));
        aggregators.add(new AggregateDescriptor("requirement_documents", Arrays.asList(requirementType)));
    }

    public static OctaneEntityTypeDescriptor getByTypeName(String key) {
        OctaneEntityTypeDescriptor desc = typeDescriptorsByName.get(key);
        if (desc == null) {
            desc = typeDescriptorsByAlias.get(key);
        }
        return desc;
    }

    public static OctaneEntityTypeDescriptor getByLabel(String label) {
        return typeDescriptorsByLabel.get(label);
    }

    public static Collection<String> getSupportedTypes() {
        return typeDescriptorsByName.keySet();
    }

    public static List<AggregateDescriptor> getAggregators() {
        return aggregators;
    }
}
