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

import java.util.*;

public class OctaneEntityTypeManager {

    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByName = new HashMap<>();
    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByLabel = new HashMap<>();
    private static List<AggregateDescriptor> aggregators = new ArrayList<>();

    static {
        //TYPE
        OctaneEntityTypeDescriptor applicationModuleType = new OctaneEntityTypeDescriptor("application_module", "AM", "Application Module", "#43e4ff", "product_area", "tests-in-pa", "product_areas", true);
        OctaneEntityTypeDescriptor featureType = new OctaneEntityTypeDescriptor("feature", "F", "Feature", "#e57828", "work_item", "tests_in_backlog", "covered_content", false);
        OctaneEntityTypeDescriptor storyType = new OctaneEntityTypeDescriptor("story", "US", "User Story", "#ffb000", "work_item", "tests_in_backlog", "covered_content", false);
        OctaneEntityTypeDescriptor requirementType = new OctaneEntityTypeDescriptor("requirement_document", "RD", "Requirement", "#0b8eac", "requirement", "tests_in_requirement", "covered_requirement", true);

        Arrays.asList(applicationModuleType, featureType, storyType, requirementType).forEach(descriptor -> {
            typeDescriptorsByName.put(descriptor.getTypeName(), descriptor);
            typeDescriptorsByLabel.put(descriptor.getLabel(), descriptor);
        });

        aggregators.add(new AggregateDescriptor("work_items", Arrays.asList(featureType, storyType)));
        aggregators.add(new AggregateDescriptor("application_modules", Arrays.asList(applicationModuleType)));
        aggregators.add(new AggregateDescriptor("requirement_documents", Arrays.asList(requirementType)));
    }

    public static OctaneEntityTypeDescriptor getByTypeName(String key) {
        return typeDescriptorsByName.get(key);
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
