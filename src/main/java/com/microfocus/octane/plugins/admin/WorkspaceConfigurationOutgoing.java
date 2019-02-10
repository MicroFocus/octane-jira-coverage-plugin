package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceConfigurationOutgoing {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "workspaceName")
    private String workspaceName;

    @XmlElement(name = "octaneField")
    private String octaneField;

    @XmlElement(name = "octaneEntityTypes")
    private List<String> octaneEntityTypes = Collections.emptyList();

    @XmlElement(name = "jiraIssueTypes")
    private List<String> jiraIssueTypes = Collections.emptyList();

    @XmlElement(name = "jiraProjects")
    private List<String> jiraProjects = Collections.emptyList();

    public WorkspaceConfigurationOutgoing() {

    }

    public WorkspaceConfigurationOutgoing(String id, String workspaceName, String octaneField, List<String> octaneEntityTypes, List<String> jiraIssueTypes, List<String> jiraProjects)
    {
        setId(id);
        setWorkspaceName(workspaceName);
        setOctaneField(octaneField);
        setOctaneEntityTypes(octaneEntityTypes);
        setJiraIssueTypes(jiraIssueTypes);
        setJiraProjects(jiraProjects);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getOctaneField() {
        return octaneField;
    }

    public void setOctaneField(String octaneField) {
        this.octaneField = octaneField;
    }


    public List<String> getOctaneEntityTypes() {
        return octaneEntityTypes;
    }

    public void setOctaneEntityTypes(List<String> octaneEntityTypes) {
        this.octaneEntityTypes = octaneEntityTypes;
    }

    public List<String> getJiraIssueTypes() {
        return jiraIssueTypes;
    }

    public void setJiraIssueTypes(List<String> jiraIssueTypes) {
        this.jiraIssueTypes = jiraIssueTypes;
    }

    public List<String> getJiraProjects() {
        return jiraProjects;
    }

    public void setJiraProjects(List<String> jiraProjects) {
        this.jiraProjects = jiraProjects;
    }
}
