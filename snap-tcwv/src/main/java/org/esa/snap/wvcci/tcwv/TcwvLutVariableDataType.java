package org.esa.snap.wvcci.tcwv;

/**
 * Enumeration for data types of LUT variables.
 *
 * @author olafd
 */
public enum TcwvLutVariableDataType {
    DOUBLE("double"),
    FLOAT("float"),
    INT("int"),
    LONG("long"),
    STRING("string");

    private final String name;

    TcwvLutVariableDataType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
