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

package com.microfocus.octane.plugins.configuration.adminparameters;

/**
 * Definition and validation for one admin-tunable parameter. All available parameters are declared
 * in {@link AdminParameterCatalog}.
 *
 * @param <T> the Java type of a value of this parameter
 */
public abstract class AdminParameterDefinition<T> {

    private final String name;
    private final T defaultValue;
    private final String description;

    protected AdminParameterDefinition(String name, T defaultValue, String description) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public abstract int getMinValue();

    public abstract int getMaxValue();

    /**
     * Validates and converts an already-deserialized raw value (as produced by Jackson for a
     * {@code Map<String, Object>}) to type {@code T}. Returns {@code null} if the value is missing,
     * of the wrong Java type or out of the allowed range.
     */
    public abstract T validateAndConvert(Object rawValue);

    public boolean isValid(Object rawValue) {
        return validateAndConvert(rawValue) != null;
    }
}