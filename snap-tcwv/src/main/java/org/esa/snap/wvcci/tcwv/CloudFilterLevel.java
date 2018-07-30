package org.esa.snap.wvcci.tcwv;

/**
 * Enumeration for supported sensors for TCWV retrieval
 *
 * @author olafd
 */
public enum CloudFilterLevel {
    NO_FILTER("NO_FILTER"),
    CLOUD_SURE("CLOUD_SURE"),
    CLOUD_SURE_AMBIGUOUS("CLOUD_SURE + CLOUD_AMBIGUOUS");

    private String name;

    CloudFilterLevel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
