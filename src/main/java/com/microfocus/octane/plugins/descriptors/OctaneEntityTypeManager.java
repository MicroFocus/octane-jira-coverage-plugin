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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OctaneEntityTypeManager {

    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByName = new HashMap<>();
    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptorsByLabel = new HashMap<>();

//key2LabelType.put("requirement_document", OCTANE_ENTITY_REQ_DOC);
    //String OCTANE_ENTITY_REQ_DOC = "Requirement";
    //String OCTANE_ENTITY_FEATURE = "Feature";
    //String OCTANE_ENTITY_USER_STORY = "User Story";
    //String OCTANE_ENTITY_APPLICATION_MODULE = "Application Module";


    static {
        //TYPE
        OctaneEntityTypeDescriptor applicationModuleType = new OctaneEntityTypeDescriptor("application_module", "AM","Application Module" ,"#43e4ff", "product_area", "tests-in-pa", "product_areas", true);
        OctaneEntityTypeDescriptor featureType = new OctaneEntityTypeDescriptor("feature", "F", "Feature","#e57828", "work_item", "tests_in_backlog", "covered_content", false);
        OctaneEntityTypeDescriptor storyType = new OctaneEntityTypeDescriptor("story", "US", "User Story","#ffb000", "work_item", "tests_in_backlog", "covered_content", false);

        typeDescriptorsByName.put(applicationModuleType.getTypeName(), applicationModuleType);
        typeDescriptorsByName.put(featureType.getTypeName(), featureType);
        typeDescriptorsByName.put(storyType.getTypeName(), storyType);

        typeDescriptorsByLabel.put(applicationModuleType.getLabel(), applicationModuleType);
        typeDescriptorsByLabel.put(featureType.getLabel(), featureType);
        typeDescriptorsByLabel.put(storyType.getLabel(), storyType);
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
}
