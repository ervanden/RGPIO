

package rgpioutils;

import java.util.List;


public class DeviceMessage {
 
// common fields
    
public String from;
public String to;
public String command;

// report fields

public String model;
public String uptime;
public List<String> dips;
public List<String> dops;
public List<String> aips;
public List<String> aops;

// event fields
public String pin;
public String value;

// message fields

public String message;

}
