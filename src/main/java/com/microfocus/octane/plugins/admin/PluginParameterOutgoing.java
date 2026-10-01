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

package com.microfocus.octane.plugins.admin;

import com.microfocus.octane.plugins.configuration.PluginParameter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * One row of the admin "Parameters" table: the parameter's definition (from {@link PluginParameter})
 * plus its current effective value. The admin page renders its rows, tooltips and input limits from
 * this, so nothing about a parameter is duplicated in the UI.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PluginParameterOutgoing {

    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "value")
    private int value;

    @XmlElement(name = "defaultValue")
    private int defaultValue;

    @XmlElement(name = "minValue")
    private int minValue;

    @XmlElement(name = "maxValue")
    private int maxValue;

    @XmlElement(name = "description")
    private String description;

    public PluginParameterOutgoing() {
    }

    public PluginParameterOutgoing(PluginParameter parameter, int value) {
        this.name = parameter.name();
        this.value = value;
        this.defaultValue = parameter.getDefaultValue();
        this.minValue = parameter.getMinValue();
        this.maxValue = parameter.getMaxValue();
        this.description = parameter.getDescription();
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
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
}
