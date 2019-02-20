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

import java.util.HashMap;
import java.util.Map;

public class OctaneEntityTypeManager {

    private static Map<String, OctaneEntityTypeDescriptor> typeDescriptors = new HashMap<>();

    static {
        //TYPE
        OctaneEntityTypeDescriptor applicationModuleType = new OctaneEntityTypeDescriptor("application_module", "AM", "#43e4ff", "product_area", "tests-in-pa","product_areas", true);
        OctaneEntityTypeDescriptor featureType = new OctaneEntityTypeDescriptor("feature", "F", "#e57828", "work_item", "tests_in_backlog","covered_content",false);
        OctaneEntityTypeDescriptor storyType = new OctaneEntityTypeDescriptor("story", "US", "#ffb000", "work_item", "tests_in_backlog","covered_content",false);
        typeDescriptors.put(applicationModuleType.getTypeName(), applicationModuleType);
        typeDescriptors.put(featureType.getTypeName(), featureType);
        typeDescriptors.put(storyType.getTypeName(), storyType);
    }

    public static OctaneEntityTypeDescriptor getByTypeName(String key){
        return typeDescriptors.get(key);
    }
}
