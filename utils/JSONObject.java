package utils;

public class JSONObject {

    String s;

    public JSONObject() {
        s = "{";
    }

    public void addProperty(String name, String value) {
        if (!s.equals("{")) {
            s = s + ",";
        };
        s = s + " \"" + name + "\":\"" + value + "\"";
    }

    public String asString() {
        return s + "}";
    }

}
