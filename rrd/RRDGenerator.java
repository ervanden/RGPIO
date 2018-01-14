package rrd;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Util;


/* this class makes it simpler for RGPIO to use the rrd4j library;

 createRRD() creates a RRD in the file name provided, for the data sources given as arguments
 and with the RRD time step of 'step'. RRD is now expecting an update every 'step' seconds.

 openRRD() opens the database so that entries can be added

 */
public class RRDGenerator {

    static ArrayList<String> dataSources = new ArrayList<>();
    static HashMap<String, Color> COLORS = new HashMap<>();

    public static void createRRD(String rrdPath,
            ArrayList<String> dataSources,
            int step) {

        File f = new File(rrdPath);
        if (f.exists()) {
            System.out.println("Using existing RRD : " + rrdPath);
        } else {
            println("== Creating RRD file " + rrdPath);
            RrdDef rrdDef = new RrdDef(rrdPath, Util.getTimestamp() - 1, step);
            rrdDef.setVersion(2);
            for (String dataSource : dataSources) {
                System.out.println("adding DataSource " + dataSource);
                rrdDef.addDatasource(dataSource, GAUGE, 2 * step, 0, Double.NaN);
            }

            int stepsPerHour = 3600 / step;
            rrdDef.addArchive(AVERAGE, 0.5, 1, 24*stepsPerHour);   // 1 day 
            rrdDef.addArchive(AVERAGE, 0.5, stepsPerHour, 365 * 24);  // 1 year hourly averages

        //println(rrdDef.dump());
            println("Estimated file size: " + rrdDef.getEstimatedSize());
            try (RrdDb rrdDb = new RrdDb(rrdDef)) {
                if (rrdDb.getRrdDef().equals(rrdDef)) {
                    println("Checking RRD file structure... OK");
                } else {
                    println("Invalid RRD file created. Giving up...");
                    return;
                }
            } catch (IOException ioe) {
            };
            println("== RRD file created.");
        }
    }

    static public RrdDb openRRD(String rrdPath) {
        try {
            RrdDb RRDB = new RrdDb(rrdPath);
            return RRDB;
        } catch (IOException ioe) {
            System.out.println("Problem opening RRD " + rrdPath);
        }
        return null;
    }

    static void println(String msg) {
        //System.out.println(msg + " " + Util.getLapTime());
        System.out.println(msg);
    }

    static void print(String msg) {
        System.out.print(msg);
    }

}
