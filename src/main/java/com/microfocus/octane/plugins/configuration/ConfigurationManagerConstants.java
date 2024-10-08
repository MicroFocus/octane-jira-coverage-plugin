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
package com.microfocus.octane.plugins.configuration;

public class ConfigurationManagerConstants {

    public static final String PLUGIN_PREFIX = "com.microfocus.octane.plugins.";

    public static final String CONFIGURATION_KEY_V1 = PLUGIN_PREFIX + "configuration";
    public static final String CONFIGURATION_KEY_V2 = PLUGIN_PREFIX + "configuration_v2";
    public static final String CONFIGURATION_KEY_V3 = PLUGIN_PREFIX + "configuration_v3";
    public static final String USER_FILTER_KEY = PLUGIN_PREFIX + "user.filter";

    public static final String MESSAGE_CHANNEL = "OCTANE_CONFIG";

    public static final int CONFIGURATION_HARD_LIMIT_SIZE = 240000;
}
