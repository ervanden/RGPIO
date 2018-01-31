package mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class MailGenerator {

    public static void sendMail(String to, String subject, String content) {
        String command = "";
        String tmpFile = "";
        Process p;
        try {

            if (System.getProperty("file.separator").equals("/")) {
                tmpFile = "/tmp/mailfile.txt";
                command = "/home/pi/git/run RGPIOMail";
            } else {
                tmpFile = "C:\\Windows\\Temp\\mailfile.txt";
                command = "java -jar C:\\Users\\erikv\\Documents\\NetBeansProjects\\RGPIOMail\\dist\\RGPIOMail.jar";
            }
            BufferedWriter bw = null;
            FileWriter fw = null;

            fw = new FileWriter(tmpFile);
            bw = new BufferedWriter(fw);
            bw.write(to);
            bw.newLine();
            bw.write(subject);
            bw.newLine();
            bw.write(content);
            bw.newLine();
            bw.close();
            fw.close();

            command = command + " mailfile=" + tmpFile;

            System.out.println("excuting " + command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();

            // read stdout and print is so we know what happened
            
            BufferedReader reader
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (InterruptedException e) {
            System.out.println("ERROR : could not execute command " + command);
        } catch (IOException e) {
            System.out.println("ERROR : could not read RGPIOMail output");
        }
    }
}
