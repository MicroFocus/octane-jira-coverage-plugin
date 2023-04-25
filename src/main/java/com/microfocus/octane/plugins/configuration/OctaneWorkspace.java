package com.microfocus.octane.plugins.configuration;

import java.util.Objects;

public class OctaneWorkspace {

    private String id;
    private String name;

    public OctaneWorkspace() {
    }

    public OctaneWorkspace(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 31 * result + id.hashCode();
        result = 31 * result + name.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null || that.getClass() != this.getClass()) {
            return false;
        }

        final OctaneWorkspace other = (OctaneWorkspace) that;
        if (!Objects.equals(this.id, other.id) || !Objects.equals(this.name, other.name)) {
            return false;
        }

        return true;
    }
}
