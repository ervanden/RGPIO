package rrd;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdSafeFileBackend;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;

public class RRDGeneratorMain {

    static ArrayList<String> SENSORS = new ArrayList<>();
    static HashMap<String, Color> COLORS = new HashMap<>();

    static void println(String msg) {
        System.out.println(msg);
    }

    static void print(String msg) {
        System.out.print(msg);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");

        if (args.length > 0) {
            println("Setting default backend factory to " + args[0]);
            RrdDb.setDefaultFactory(args[0]);
        }

        SENSORS.add("R1front");
        SENSORS.add("R2front");
        SENSORS.add("R3front");
        SENSORS.add("R4front");
        COLORS.put("R1front", Color.red);
        COLORS.put("R2front", Color.blue);
        COLORS.put("R3front", Color.cyan);
        COLORS.put("R4front", Color.black);

        String RRDDIRECTORY = "C:\\Users\\erikv\\Documents\\RRD\\";
        String RRDNAME = "datastore";
        String rrdPath = RRDDIRECTORY + RRDNAME + ".rrd";
        RRDGenerator.createRRD(rrdPath,SENSORS,COLORS,300);
        RrdDb rrddb = RRDGenerator.openRRD(rrdPath);

        // can now update database
        ArrayList<GaugeSource> gaugeSources = new ArrayList<>();
        int s = 0;
        for (String sensor : SENSORS) {
            gaugeSources.add(new GaugeSource(1909752002L + (s++), 20));
        }

        /* RRD updates with random step not larger than MAX_STEP  seconds */
        long SEED = 1909752002L;
        Random RANDOM = new Random(SEED);
        long START = Util.getTimestamp();
        long END = Util.getTimestamp() + 2 * 30 * 24 * 60 * 60;
        int MAX_STEP = 599;

        Sample sample = rrddb.createSample();
        long t = START;
        int n = 0;
        while (t <= END + 172800L) {
            sample.setTime(t);
            int i = 0;
            for (String sensor : SENSORS) {
                sample.setValue(sensor, gaugeSources.get(i).getValue());
                i++;
            }
            sample.update();
            t += RANDOM.nextDouble() * MAX_STEP + 1;
        }

        println("");
        println("== Finished. RRD file updated " + n + " times");

        // create graph
        String imgPath = RRDGenerator.createGraph(rrdPath, Util.getTimestamp(), Util.getTimestamp() + 1 * 30 * 24 * 60 * 60);

        // locks info
        println("== Locks info ==");
        println(RrdSafeFileBackend.getLockInfo());

    }

    static class GaugeSource {

        double value;
        double slope = 0;
        int countdown = 0;
        Random RANDOM;

        GaugeSource(long seed, double value) {
            RANDOM = new Random(seed);
            this.value = value;
        }

        long getValue() {
            if (countdown == 0) {
                // new slope and countdown     
                slope = (RANDOM.nextDouble() - 0.5);
                countdown = RANDOM.nextInt(5) + 1;
            }
            value = value + slope * 0.01;
//           System.out.println(slope + "\t" + value);
            countdown--;
            return Math.round(value * 10);
        }

    }

}
