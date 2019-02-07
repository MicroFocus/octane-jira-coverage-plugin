package com.microfocus.octane.plugins.admin;

public class Select2ResultItem {

    private String id;
    private String text;

    public Select2ResultItem(String id, String text){
        this.text = text;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }
}
