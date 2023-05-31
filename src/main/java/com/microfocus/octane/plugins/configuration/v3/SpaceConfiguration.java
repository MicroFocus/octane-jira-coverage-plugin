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

package com.microfocus.octane.plugins.configuration.v3;


import com.microfocus.octane.plugins.configuration.LocationParts;
import com.microfocus.octane.plugins.configuration.OctaneRestManager;
import com.microfocus.octane.plugins.rest.RestConnector;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpaceConfiguration {

    private String name;
    private String location;
    private LocationParts locationParts;
    private String clientId;
    private String clientSecret;
    private String id;

    @JsonIgnore
    private RestConnector restConnector;

    public SpaceConfiguration() {
    }

    public SpaceConfiguration(String name, String location, LocationParts locationParts, String clientId, String clientSecret, String id) {
        this.name = name;
        this.location = location;
        this.locationParts = locationParts;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocationParts getLocationParts() {
        return locationParts;
    }

    public void setLocationParts(LocationParts locationParts) {
        this.locationParts = locationParts;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public RestConnector getRestConnector() {
        if (restConnector == null) {
            restConnector = OctaneRestManager.getRestConnector(getLocationParts().getBaseUrl(), getClientId(), getClientSecret());
        }
        return restConnector;
    }

    public void clearRestConnector() {
        restConnector = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpaceConfiguration that = (SpaceConfiguration) o;
        return location.equals(that.location) && locationParts.equals(that.locationParts) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, locationParts, id);
    }
}
