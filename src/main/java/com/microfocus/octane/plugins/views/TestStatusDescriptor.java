package com.microfocus.octane.plugins.views;

public class TestStatusDescriptor {

    private String logicalName;
    private String title;
    private String color;
    private String key;
    private int order;

    public TestStatusDescriptor(String logicalName, String key, String title, String color, int order) {
        this.logicalName = logicalName;
        this.title = title;
        this.color = color;
        this.order = order;
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public String getLogicalName() {
        return logicalName;
    }

    public String getColor() {
        return color;
    }

    public int getOrder() {
        return order;
    }

    public String getKey() {
        return key;
    }
}
