package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceConfigurationModel {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "workspaceName")
    private String workspaceName;

    @XmlElement(name = "octaneField")
    private String octaneField;

    public WorkspaceConfigurationModel() {

    }

    public WorkspaceConfigurationModel(String id, String workspaceName, String octaneField)
    {
        setId(id);
        setWorkspaceName(workspaceName);
        setOctaneField(octaneField);
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

}
