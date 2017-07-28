package rgpio;

public class VDigitalOutput extends VOutput {

    public Boolean getState() {
        if (value.equals("High")) {
            return true;
        } else if (value.equals("Low")) {
            return false;
        } else {
            System.out.println("Invalid value of digital output : " + value);
            return null;
        }
    }

    public void setState(boolean state) {
        if (state) {
            set("High");
        } else {
            set("Low");
        }
    }
}
