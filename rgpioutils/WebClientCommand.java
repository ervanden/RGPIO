package rgpioutils;

public class WebClientCommand {

    public String Command;
    public String Arg1;
    public String Arg2;
    public String Arg3;

    public String toString() {
        String s = "";
        s = s + "{\n";
        s = s + "Command : " + Command + "\n";
        s = s + "Arg1 : " + Arg1 + "\n";
        s = s + "Arg2 : " + Arg2 + "\n";
        s = s + "Arg3 : " + Arg3 + "\n";
        s = s + "}";
        return s;
    }
}
