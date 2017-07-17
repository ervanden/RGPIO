package utils;

import java.util.HashSet;

public class Console {   // controls if output is written to the screen or not

    private static HashSet<String> verboseCallers = new HashSet<>();

    private static String callingClass() {
        /*
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTraceElements.length; i++) {
          System.out.println("  " + i + ":" + stackTraceElements[i]);
        }
         */
        String caller = Thread.currentThread().getStackTrace()[3].getClassName();
//      System.out.println("Console called by " + caller);
        return caller;
    }

    public static void verbose(boolean v) {

        String caller = callingClass();

        if (v) {
            verboseCallers.add(caller);
        } else {
            verboseCallers.remove(caller);
        }
    }

    public static void println(String s) {

        String caller = callingClass();
        if (verboseCallers.contains(caller)) {
            System.out.println(s);
        }
    }
}
