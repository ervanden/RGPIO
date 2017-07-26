package rgpio;

public enum IOType {

    digitalInput,
    digitalOutput,
    analogInput,
    analogOutput,
    stringInput,
    stringOutput;

    public static String longToShort(IOType type) {
        String shortName = null;
        if (type == IOType.digitalInput) {
            shortName = "Dip";
        }
        if (type == IOType.analogInput) {
            shortName = "Aip";
        }
        if (type == IOType.stringInput) {
            shortName = "Sip";
        }
        if (type == IOType.digitalOutput) {
            shortName = "Dop";
        }
        if (type == IOType.analogOutput) {
            shortName = "Aop";
        }
        if (type == IOType.stringOutput) {
            shortName = "Sop";
        }
        return shortName;
    }

    public static IOType shortToLong(String shortName) {
        IOType type = null;
        if (shortName.equals("Dip")) {
            type = IOType.digitalInput;
        }
        if (shortName.equals("Aip")) {
            type = IOType.analogInput;
        }
        if (shortName.equals("Sip")) {
            type = IOType.stringInput;
        }
        if (shortName.equals("Dop")) {
            type = IOType.digitalOutput;
        }
        if (shortName.equals("Aop")) {
            type = IOType.analogOutput;
        }
        if (shortName.equals("Sop")) {
            type = IOType.stringOutput;
        }
        return type;
    }
}
