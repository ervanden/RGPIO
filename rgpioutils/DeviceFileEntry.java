package rgpioutils;

// Object with a field corresponding to all possible properties in the device file JSON objects.
// Used to parse the device file

public class DeviceFileEntry {

    public String Device;
    public String HWid;
    public String Model;
    public String DigitalInput;
    public String DigitalOutput;
    public String AnalogInput;
    public String AnalogOutput;
    public String StringInput;
    public String StringOutput;
    public String Pin;

    public String toString() {
        String s = "";
        s = s + "{\n";
        s = s + "Device : " + Device + "\n";
        s = s + "Pin : " + Pin + "\n";
        s = s + "Model : " + Model + "\n";
        s = s + "HWid : " + HWid + "\n";
        s = s + "DigitalInput : " + DigitalInput + "\n";
        s = s + "DigitalOutput : " + DigitalOutput + "\n";
        s = s + "AnalogInput : " + AnalogInput + "\n";
        s = s + "AnalogOutput : " + AnalogOutput + "\n";
        s = s + "StringInput : " + StringInput + "\n";
        s = s + "StringOutput : " + StringOutput + "\n";
        s = s + "}";
        return s;
    }
}
