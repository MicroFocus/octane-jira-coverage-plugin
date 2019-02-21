package com.microfocus.octane.plugins.admin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)

public class Select2ResultItem {

    @XmlElement(name = "id")
    private String id;

    @XmlElement(name = "text")
    private String text;


    public Select2ResultItem(String id, String text) {
        this.text = text;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id +" : " + text;
    }
}
