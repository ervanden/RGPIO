package rgpio;

import devices.TimeStamp;

public class RGPIOMessageEvent {

    public Long time = null;
    public RGPIOMessageType type;
    public String description = null;

    // device information
    public String state = null;
    public String model = null;
    public String group = null;
    public String HWid = null;
    public String ipAddress = null;

    // pin information
    public String pinType = null;
    public String pinLabel = null;

    public String digitalInput = null;
    public String digitalOutput = null;

    public RGPIOMessageEvent(RGPIOMessageType type) {
        time = new TimeStamp().getTimeInMillis();
        this.type = type;
    }

    public String toString() {

        String msg = "";

        TimeStamp timeStamp = new TimeStamp(0);
        timeStamp.setTimeInMillis(time);

        msg = msg + timeStamp.asString();
        msg = msg + " " + type.toString();
        msg = msg + " \"" + description + " \"";
        if (HWid != null) {
            msg = msg + " HWid=" + HWid;
        }
        if (model != null) {
            msg = msg + " model=" + model;
        }
        if (group != null) {
            msg = msg + " group=" + group;
        }
        if (ipAddress != null) {
            msg = msg + " ipAddress=" + ipAddress;
        }
        if (pinType != null) {
            msg = msg + " pinType=" + pinType;
        }
        if (pinLabel != null) {
            msg = msg + " pinLabel=" + pinLabel;
        }
        if (state != null) {
            msg = msg + " state=" + state;
        }
        if (digitalInput != null) {
            msg = msg + " digitalInput=" + digitalInput;
        }
        if (digitalOutput != null) {
            msg = msg + " digitalOutput=" + digitalOutput;
        }
        return msg;
    }

    public String toJSON() {

        JSONString json = new JSONString();

        TimeStamp timeStamp = new TimeStamp(0);
        timeStamp.setTimeInMillis(time);

        json.addString("time", timeStamp.asString());
        json.addString("type", type.toString());
        json.addString("description", description);
        if (HWid != null) {
            json.addString("HWid", HWid);
        }
        if (model != null) {
            json.addString("model=", model);
        }
        if (group != null) {
            json.addString("group=", group);
        }
        if (ipAddress != null) {
            json.addString("ipAddress", ipAddress);
        }
        if (pinType != null) {
            json.addString("pinType", pinType);
        }
        if (pinLabel != null) {
            json.addString("pinLabel", pinLabel);
        }
        if (state != null) {
            json.addString("state", state);
        }
        if (digitalInput != null) {
            json.addString("digitalInput", digitalInput);
        }
        if (digitalOutput != null) {
            json.addString("digitalOutput", digitalOutput);
        }
        return json.close();
    }
}
