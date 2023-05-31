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

import com.microfocus.octane.plugins.configuration.OctaneWorkspace;

import java.util.Objects;
import java.util.Set;

public class OctaneConfigGrouping {

    private Set<OctaneWorkspace> octaneWorkspaces;
    private String octaneUdf;
    private Set<String> octaneEntityTypes;

    public OctaneConfigGrouping() {
    }

    public OctaneConfigGrouping(Set<OctaneWorkspace> octaneWorkspaces, String octaneUdf, Set<String> octaneEntityTypes) {
        this.octaneWorkspaces = octaneWorkspaces;
        this.octaneUdf = octaneUdf;
        this.octaneEntityTypes = octaneEntityTypes;
    }

    public Set<OctaneWorkspace> getOctaneWorkspaces() {
        return octaneWorkspaces;
    }

    public void setOctaneWorkspaces(Set<OctaneWorkspace> octaneWorkspaces) {
        this.octaneWorkspaces = octaneWorkspaces;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public void setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
    }

    public Set<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public void setOctaneEntityTypes(Set<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + octaneWorkspaces.hashCode();
        result = 31 * result + octaneUdf.hashCode();
        result = 31 * result + octaneEntityTypes.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null || that.getClass() != this.getClass()) {
            return false;
        }

        final OctaneConfigGrouping other = (OctaneConfigGrouping) that;
        if (!Objects.equals(this.octaneWorkspaces, other.octaneWorkspaces)
                || !Objects.equals(this.octaneUdf, other.octaneUdf)
                || !Objects.equals(this.octaneEntityTypes,other.octaneEntityTypes)) {
            return false;
        }

        return true;
    }
}
