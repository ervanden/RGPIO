package rgpio;

import java.util.ArrayList;
import java.util.List;

/* How to use the RGPIOTimer

 lightsOut = new RGPIOTimer("LightsOut", true);
 lightsOut.addListener(this);
 lightsOut.start();  // nothing happens until timer is set
...
 lightsOut.set(10);
...
 lightsOut.cancel();

*/

public class RGPIOTimer extends Thread {

    int timeOut;
    String name;
    boolean verbose;

    public RGPIOTimer(String name, boolean verbose) {
        super();
        this.timeOut = -1;
        this.name = name;
        this.verbose = verbose;
    }

    public void run() {
        while (true) {
            if ((timeOut > 0) && verbose) {
                System.out.println("Timer " + name + " : timeout in " + timeOut + " seconds ");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                System.out.println("!! sleep was interrupted");
            }
            if (timeOut > 0) {
                timeOut--;
            }
            if (timeOut == 0) {
                if (verbose) {
                    System.out.println("Timer " + name + " timed out");
                }
                timeOut = -1;

                //  Call the listeners that implement the action on timeout
                for (RGPIOTimerListener l : listeners) {
                    try {
                        l.onTimeOut(name);
                    } catch (Exception ex) {
                    };
                }
            }
        }
    }

    public void cancel() {
        timeOut = -1;
        if (verbose) {
            System.out.println("Timer " + name + " cancelled");
        }
    }

    public void set(int seconds) {
        timeOut = seconds;
        if (verbose) {
            System.out.println("Timer " + name + " set to " + seconds);
        }
    }

    private static List<RGPIOTimerListener> listeners = new ArrayList<>();

    public static void addListener(RGPIOTimerListener toAdd) {
        listeners.add(toAdd);
    }

}
