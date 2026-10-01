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
 */
public enum PluginParameter {

    TEST_COVERAGE_CACHE_DURATION_IN_SECONDS(300, 0, 86400,
            "How long (in seconds) coverage data already fetched from Core Software Delivery Platform is reused by the issue panel "
                    + "(e.g. when the panel is redrawn after the issue is saved, or when switching back to a workspace) before it is fetched again. "
                    + "Users can always force a fresh fetch with the panel's refresh button. "
                    + "Set to 0 to disable caching: the panel then always shows up-to-date data, at the cost of more requests to Core Software Delivery Platform."),

    MAX_CACHED_ENTITIES_WITH_TEST_COVERAGE(40, 1, 5000,
            "Maximum number of distinct Jira issues whose coverage data each user's browser keeps cached at the same time "
                    + "(per user, per browser tab - nothing is shared between users, and the cache is emptied on a full page reload). "
                    + "Once the limit is reached, the data of the least recently viewed issue is discarded first.");

    private final int defaultValue;
    private final int minValue;
    private final int maxValue;
    private final String description;

    PluginParameter(int defaultValue, int minValue, int maxValue, String description) {
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

    public static Optional<PluginParameter> byName(String name) {
        return Arrays.stream(values()).filter(p -> p.name().equals(name)).findFirst();
    }
}
