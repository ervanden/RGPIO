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
}
