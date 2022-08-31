package com.microfocus.octane.plugins.configuration;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionEntity {

    private String version;
    private String buildDate;
    private String buildRevision;
    private String buildNumber;
    private String displayVersion;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildDate() {
        return buildDate;
    }

    @JsonProperty("build_date")
    public void setBuildDate(String buildDate) {
        this.buildDate = buildDate;
    }

    public String getBuildRevision() {
        return buildRevision;
    }

    @JsonProperty("build_revision")
    public void setBuildRevision(String buildRevision) {
        this.buildRevision = buildRevision;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    @JsonProperty("build_number")
    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getDisplayVersion() {
        return displayVersion;
    }

    @JsonProperty("display_version")
    public void setDisplayVersion(String displayVersion) {
        this.displayVersion = displayVersion;
    }
}
