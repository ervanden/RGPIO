package utils;

public class JSONString {

    String s;

    public JSONString() {
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
