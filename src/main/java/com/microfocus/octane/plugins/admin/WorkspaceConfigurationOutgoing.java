package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

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
    private List<String> octaneEntityTypes;

    @XmlElement(name = "jiraIssueTypes")
    private List<String> jiraIssueTypes;

    @XmlElement(name = "jiraProjects")
    private List<String> jiraProjects;

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


    public List<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public WorkspaceConfigurationOutgoing setOctaneEntityTypes(List<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
        return this;
    }

    public List<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public WorkspaceConfigurationOutgoing setJiraIssueTypes(List<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
        return this;
    }

    public List<String> getJiraProjects() {
        return jiraProjects;
    }

    public WorkspaceConfigurationOutgoing setJiraProjects(List<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
        return this;
    }
}
