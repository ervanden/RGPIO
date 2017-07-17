package rgpio;

public class JSONString {

    String s;

    public JSONString() {
        s = "{";
    }

    public void addString(String name, String value) {
        if (!s.equals("{")) {
            s = s + ",";
        };
        s = s + " \"" + name + "\":\"" + value + "\"";
    }

    public String close() {
        return s + "}";
    }

}
