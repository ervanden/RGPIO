package mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MailGenerator {

  public static  void sendMail(String to, String subject, String content) {
        String command = "";
        String args = " to=\""+to+"\" subject=\""+subject+"\" content=\""+content+"\"";
        Process p;
        try {

            if (System.getProperty("file.separator").equals("/")) {
                command = "/home/pi/git/run RGPIOMail " + args;
            } else {
                command = "java -jar C:\\Users\\erikv\\Documents\\NetBeansProjects\\RGPIOMail\\dist\\RGPIOMail.jar " + args;
            }
            System.out.println("excuting " + command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();

            BufferedReader reader
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (InterruptedException e) {
            System.out.println("ERROR : could not execute command " + command);
        } catch (IOException e) {
            System.out.println("ERROR : could not read RGPIOGraph output");
        }
    }
}
