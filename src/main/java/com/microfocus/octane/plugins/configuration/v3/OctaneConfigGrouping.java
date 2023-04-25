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
