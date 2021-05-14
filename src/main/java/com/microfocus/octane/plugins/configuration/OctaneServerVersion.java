package com.microfocus.octane.plugins.configuration;

import org.apache.commons.lang.Validate;

public class OctaneServerVersion implements Comparable<OctaneServerVersion> {

    private int releaseVersion;
    private int minorVersion;
    private int pushNumber;
    private int buildNumber;

    public OctaneServerVersion(String version) {
        String[] versionNumbers = version.split("\\.");
        if ((versionNumbers.length != 3) && (versionNumbers.length != 4)) {
            throw new RuntimeException(String.format("The version '%s' does not have a correct format", version));
        }
        releaseVersion = Integer.parseInt(versionNumbers[0]);
        minorVersion = Integer.parseInt(versionNumbers[1]);
        pushNumber = Integer.parseInt(versionNumbers[2]);
        if (versionNumbers.length == 4) {
            buildNumber = Integer.parseInt(versionNumbers[3]);
        } else {
            buildNumber = Integer.parseInt(PluginConstants.DEFAULT_BUILD);
        }
    }

    public int getReleaseVersion() {
        return releaseVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public int getPushNumber() {
        return pushNumber;
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OctaneServerVersion)) return false;

        OctaneServerVersion version = (OctaneServerVersion) o;

        if (releaseVersion != version.releaseVersion) return false;
        if (minorVersion != version.minorVersion) return false;
        return pushNumber == version.pushNumber;

    }

    @Override
    public int hashCode() {
        int result = releaseVersion;
        result = 31 * result + minorVersion;
        result = 31 * result + pushNumber;
        return result;
    }

    @Override
    //build number is not part of version compare
    //upgrade is required for push number change or higher
    public int compareTo(OctaneServerVersion version) {
        Validate.notNull(version, "Argument should not be null.");
        if (getReleaseVersion() > version.getReleaseVersion()) {
            return 1;
        }
        if (getReleaseVersion() < version.getReleaseVersion()) {
            return -1;
        }

        if (getMinorVersion() > version.getMinorVersion()) {
            return 1;
        }

        if (getMinorVersion() < version.getMinorVersion()) {
            return -1;
        }

        if (getPushNumber() > version.getPushNumber()) {
            return 1;
        }

        if (getPushNumber() < version.getPushNumber()) {
            return -1;
        }
        return 0;
    }

    public boolean isGreaterThan(OctaneServerVersion version) {
        return compareTo(version) > 0;
    }

    public boolean isGreaterOrEqual(OctaneServerVersion version) {
        return compareTo(version) >= 0;
    }

    public boolean isLessThan(OctaneServerVersion version) {
        return compareTo(version) < 0;
    }

    public boolean isLessOrEqual(OctaneServerVersion version) {
        return compareTo(version) <= 0;
    }

    public String getProductVersion() {
        return releaseVersion + PluginConstants.SEPARATOR +
                minorVersion + PluginConstants.SEPARATOR +
                pushNumber;
    }

    @Override
    public String toString() {
        return "Version{" +
                "releaseVersion=" + releaseVersion +
                ", minorVersion=" + minorVersion +
                ", pushNumber='" + pushNumber + '\'' +
                ", buildNumber=" + buildNumber +
                '}';
    }
}