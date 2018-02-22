package tcputils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;

public class TelnetCommand implements TelnetNotificationHandler {

    public String remoteip = null;
    public Integer remoteport = null;
    public String userprompt = null;
    public String username = null;
    public String passwordprompt = null;
    public String password = null;
    public String commandprompt = null;

    public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis)
            throws IOException {
        int bufferOffset = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
            int readLength = java.lang.Math.min(is.available(), b.length - bufferOffset);
            // can alternatively use bufferedReader, guarded by isReady():
            int readResult = is.read(b, bufferOffset, readLength);
            if (readResult == -1) {
                break;
            }
            bufferOffset += readResult;
        }
        return bufferOffset;
    }

    public String session(String command) {

        OutputStream outstr;
        InputStream instr;

        TelnetClient tc = new TelnetClient();
        String result = "";

        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);

        try {
            tc.addOptionHandler(ttopt);
            tc.addOptionHandler(echoopt);
            tc.addOptionHandler(gaopt);
        } catch (InvalidTelnetOptionException e) {
            System.err.println("Error registering option handlers: " + e.getMessage());
        }

        try {
            tc.connect(remoteip, remoteport);

            tc.registerNotifHandler(new TelnetCommand());

            outstr = tc.getOutputStream();
            instr = tc.getInputStream();

            boolean commandSent = false;
            boolean commandExecuted = false;
            boolean abortSession = false;
            try {
                byte[] buff = new byte[1024];
                int ret_read = 0;

                do {
                    ret_read = readInputStreamWithTimeout(instr, buff, 1000);

                    if (ret_read > 0) {
                        String reply = new String(buff, 0, ret_read);
//                      System.out.print("|" + reply + "|" + " " + reply.length());
//                      System.out.print(reply);
                        if (commandSent) {
                            result = result + reply;
                        }
                        if (reply.endsWith(userprompt)) {
                            String rnusername = username + "\r\n";
                            outstr.write(rnusername.getBytes(), 0, rnusername.length());
                            outstr.flush();
                        } else if (reply.endsWith(passwordprompt)) {
                            String rnpassword = password + "\r\n";
                            outstr.write(rnpassword.getBytes(), 0, rnpassword.length());
                            outstr.flush();
                        } else if (reply.endsWith(commandprompt)) {
//                            System.out.println("command prompt received");
                            if (!commandSent) {
//                                System.out.println("sending command");
                                String rncommand = command + "\r\n";
                                outstr.write(rncommand.getBytes(), 0, rncommand.length());
                                outstr.flush();
                                commandSent = true;
                            } else {
//                                System.out.println("command executed");
                                commandExecuted = true;
                            }
                        } else {
                            System.err.println("Unexpected output from telnet:");
                            System.err.println(reply);
                            abortSession = true;
                        }
                    }
                } while ((ret_read >= 0) && !commandExecuted && !abortSession);

            } catch (IOException e) {
                System.err.println("Exception while reading socket:" + e.getMessage());
            }

            try {
                tc.disconnect();
            } catch (IOException e) {
                System.err.println("Exception while closing telnet:" + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Exception while connecting:" + e.getMessage());
        }
        return result;
    }

    @Override
    public void receivedNegotiation(int negotiation_code, int option_code) {
    }

}
