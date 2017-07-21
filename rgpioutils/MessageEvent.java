package rgpioutils;

import utils.JSONObject;
import utils.TimeStamp;

public class MessageEvent {

    public Long time = null;
    public MessageType type;
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

    public MessageEvent(MessageType type) {
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

        JSONObject json = new JSONObject();

        TimeStamp timeStamp = new TimeStamp(0);
        timeStamp.setTimeInMillis(time);

        json.addProperty("time", timeStamp.asString());
        json.addProperty("type", type.toString());
        json.addProperty("description", description);
        if (HWid != null) {
            json.addProperty("HWid", HWid);
        }
        if (model != null) {
            json.addProperty("model=", model);
        }
        if (group != null) {
            json.addProperty("group=", group);
        }
        if (ipAddress != null) {
            json.addProperty("ipAddress", ipAddress);
        }
        if (pinType != null) {
            json.addProperty("pinType", pinType);
        }
        if (pinLabel != null) {
            json.addProperty("pinLabel", pinLabel);
        }
        if (state != null) {
            json.addProperty("state", state);
        }
        if (digitalInput != null) {
            json.addProperty("digitalInput", digitalInput);
        }
        if (digitalOutput != null) {
            json.addProperty("digitalOutput", digitalOutput);
        }
        return json.asString();
    }
}
