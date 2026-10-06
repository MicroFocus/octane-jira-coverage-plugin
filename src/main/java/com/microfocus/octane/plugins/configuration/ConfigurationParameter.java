/*******************************************************************************
 * Copyright 2017-2026 Open Text.
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

package com.microfocus.octane.plugins.configuration;

import java.util.Arrays;
import java.util.Optional;

/**
 * Single source of truth for the admin-tunable parameters ("Parameters" section of the admin page):
 * name, default, allowed range and description. Everything else is derived from this enum - the admin
 * page rows, the REST validation, the persisted values and what the coverage panel receives.
 * <p>
 * Adding a parameter = adding one constant here, plus the code that actually uses the value.
 * <p>
 * Intentionally integer-only for now: every parameter introduced so far has been a bounded whole
 * number, so this enum models that one real case instead of a speculative generic class hierarchy
 * (e.g. a type registry covering String/Boolean/enum-of-choices) for types nothing currently needs.
 * Each of those other types would want its own validation/UI story (allowed values vs. a numeric
 * range, for example), so designing that abstraction now would mean guessing its shape rather than
 * deriving it from a real second case. If/when a non-integer parameter is actually needed, revisit
 * this as a deliberate refactor informed by that concrete requirement.
 */
public enum ConfigurationParameter {

    TEST_COVERAGE_CACHE_DURATION_IN_SECONDS(300, 0, 86400,
            "How long (in seconds) coverage data already fetched from Core Software Delivery Platform is reused by the issue panel "
                    + "(e.g. when the panel is redrawn after the issue is saved, or when switching back to a workspace) before it is fetched again. "
                    + "Users can always force a fresh fetch with the panel's refresh button. "
                    + "Set to 0 to disable caching: the panel then always shows up-to-date data, at the cost of more requests to Core Software Delivery Platform.");

    private final int defaultValue;
    private final int minValue;
    private final int maxValue;
    private final String description;

    ConfigurationParameter(int defaultValue, int minValue, int maxValue, String description) {
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.description = description;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isValid(Integer value) {
        return value != null && value >= minValue && value <= maxValue;
    }

    public static Optional<ConfigurationParameter> getParameterByName(String name) {
        return Arrays.stream(values()).filter(p -> p.name().equals(name)).findFirst();
    }
}
