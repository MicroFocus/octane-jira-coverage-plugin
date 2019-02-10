package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SpaceConfigurationOutgoing {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "location")
    private String location;

    @XmlElement(name = "clientId")
    private String clientId;

    @XmlElement(name = "clientSecret")
    private String clientSecret;

    public static SpaceConfigurationOutgoing create(String id, String location, String clientId, String clientSecret){
        SpaceConfigurationOutgoing model = new SpaceConfigurationOutgoing();
        model.setId(id);
        model.setLocation(location);
        model.setClientId(clientId);
        model.setClientSecret(clientSecret);
        return model;
    }

    public String getId() {
        return id;
    }

    public SpaceConfigurationOutgoing setId(String id) {
        this.id = id;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getClientId() {
        return clientId;
    }

    public void  setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void  setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
