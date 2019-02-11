package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceConfigurationOutgoing {

    @XmlElement(name = "id")
    private long id;

    @XmlElement(name = "workspaceName")
    private String workspaceName;

    @XmlElement(name = "octaneUdf")
    private String octaneUdf;

    @XmlElement(name = "octaneEntityTypes")
    private Set<String> octaneEntityTypes;

    @XmlElement(name = "jiraIssueTypes")
    private Set<String> jiraIssueTypes;

    @XmlElement(name = "jiraProjects")
    private Set<String> jiraProjects;

    public long getId() {
        return id;
    }

    public WorkspaceConfigurationOutgoing setId(long id) {
        this.id = id;
        return this;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public WorkspaceConfigurationOutgoing setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
        return this;
    }

    public String getOctaneUdf() {
        return octaneUdf;
    }

    public WorkspaceConfigurationOutgoing setOctaneUdf(String octaneUdf) {
        this.octaneUdf = octaneUdf;
        return this;
    }


    public Set<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfigurationOutgoing setOctaneEntityTypes(Set<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public Set<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfigurationOutgoing setJiraIssueTypes(Set<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public Set<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfigurationOutgoing setJiraProjects(Set<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }
}
