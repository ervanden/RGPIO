package rrd;

import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.DsType.GAUGE;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Util;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.TimeLabelFormat;

public class RRDGenerator {

    static ArrayList<String> SENSORS = new ArrayList<>();
    static HashMap<String, Color> COLORS = new HashMap<>();
    /*
     static void init(ArrayList<String> sensors, HashMap<String, Color> colors) {
     SENSORS = sensors;
     COLORS = colors;
     }
     */

    public static void createRRD(String rrdPath, 
            ArrayList<String> sensors, 
            HashMap<String, Color> colors, 
            int step) {

        SENSORS = sensors;
        COLORS = colors;

        println("== Creating RRD file " + rrdPath);
        RrdDef rrdDef = new RrdDef(rrdPath, Util.getTimestamp() - 1, step);
        rrdDef.setVersion(2);
        for (String sensor : SENSORS) {
            System.out.println("addDataSource for " + sensor);
            rrdDef.addDatasource(sensor, GAUGE, 2*step, 0, Double.NaN);
        }

        rrdDef.addArchive(AVERAGE, 0.5, 1, 3600/step);  // 1 hour
        rrdDef.addArchive(AVERAGE, 0.5, 3600/step, 24);  // 1 day
        rrdDef.addArchive(AVERAGE, 0.5, 30*3600/step, 30);  // 1 month

        println(rrdDef.dump());

        println("Estimated file size: " + rrdDef.getEstimatedSize());
        try (RrdDb rrdDb = new RrdDb(rrdDef)) {
            if (rrdDb.getRrdDef().equals(rrdDef)) {
                println("Checking RRD file structure... OK");
            } else {
                println("Invalid RRD file created. This is a serious bug, bailing out");
                return;
            }
        } catch (IOException ioe) {
        };
        println("== RRD file created.");

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

    static String createGraph(String rrdPath, long start, long end) {
        int IMG_WIDTH = 900;
        int IMG_HEIGHT = 500;
        String imgPath = rrdPath.replaceAll(".rrd", ".png");
        RrdGraphDef gDef = new RrdGraphDef();
        gDef.setTimeLabelFormat(new CustomTimeLabelFormat());
        gDef.setLocale(Locale.US);
        gDef.setWidth(IMG_WIDTH);
        gDef.setHeight(IMG_HEIGHT);

        gDef.setFilename(imgPath);
        gDef.setStartTime(start);
        gDef.setEndTime(end);
        gDef.setTitle("Temperatures in the next two months");
        gDef.setVerticalLabel("temperature");

        for (String sensor : SENSORS) {
            gDef.datasource(sensor, rrdPath, sensor, AVERAGE);
            gDef.line(sensor, COLORS.get(sensor), sensor);
        }

        gDef.comment("\\r");

        gDef.setImageInfo("<img src='%s' width='%d' height = '%d'>");
        gDef.setPoolUsed(false);
        gDef.setImageFormat("png");
        gDef.setDownsampler(new eu.bengreen.data.utility.LargestTriangleThreeBuckets((int) (IMG_WIDTH * 1)));
        println("Rendering graph " + Util.getLapTime());
        // create graph finally
        try {
            RrdGraph graph = new RrdGraph(gDef);
            println(graph.getRrdGraphInfo().dump());
        } catch (IOException ioe) {
            imgPath = null;
        };

        return imgPath;
    }

    static void println(String msg) {
        //System.out.println(msg + " " + Util.getLapTime());
        System.out.println(msg);
    }

    static void print(String msg) {
        System.out.print(msg);
    }

    static class CustomTimeLabelFormat implements TimeLabelFormat {

        public String format(Calendar c, Locale locale) {
            if (c.get(Calendar.MILLISECOND) != 0) {
                return String.format(locale, "%1$tH:%1$tM:%1$tS.%1$tL", c);
            } else if (c.get(Calendar.SECOND) != 0) {
                return String.format(locale, "%1$tH:%1$tM:%1$tS", c);
            } else if (c.get(Calendar.MINUTE) != 0) {
                return String.format(locale, "%1$tH:%1$tM", c);
            } else if (c.get(Calendar.HOUR_OF_DAY) != 0) {
                return String.format(locale, "%1$tH:%1$tM", c);
            } else if (c.get(Calendar.DAY_OF_MONTH) != 1) {
                return String.format(locale, "%1$td %1$tb", c);
            } else if (c.get(Calendar.DAY_OF_YEAR) != 1) {
                return String.format(locale, "%1$td %1$tb", c);
            } else {
                return String.format(locale, "%1$tY", c);
            }
        }
    }
}
