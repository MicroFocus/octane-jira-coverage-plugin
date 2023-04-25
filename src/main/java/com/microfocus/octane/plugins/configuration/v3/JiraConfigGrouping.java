package com.microfocus.octane.plugins.configuration.v3;

import com.microfocus.octane.plugins.configuration.OctaneWorkspace;

import java.util.Objects;
import java.util.Set;

public class JiraConfigGrouping {

    private Set<String> projectNames;
    private Set<String> issueTypes;

    public JiraConfigGrouping() {
    }

    public JiraConfigGrouping(Set<String> projectNames, Set<String> issueTypes) {
        this.projectNames = projectNames;
        this.issueTypes = issueTypes;
    }

    public Set<String> getProjectNames() {
        return projectNames;
    }

    public void setProjectNames(Set<String> projectNames) {
        this.projectNames = projectNames;
    }

    public Set<String> getIssueTypes() {
        return issueTypes;
    }

    public void setIssueTypes(Set<String> issueTypes) {
        this.issueTypes = issueTypes;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + projectNames.hashCode();
        result = 31 * result + issueTypes.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null || that.getClass() != this.getClass()) {
            return false;
        }

        final JiraConfigGrouping other = (JiraConfigGrouping) that;
        if (!Objects.equals(this.projectNames, other.projectNames) || !Objects.equals(this.issueTypes, other.issueTypes)) {
            return false;
        }

        return true;
    }
}
