

package rgpioutils;

import java.util.List;


public class DeviceMessage {
    
//  Report/HWid:rgpio/Model:RASPBERRY/Uptime:335/Dip:button/Aip:distance/Aip:temp/Dop:heating/Dop:boiler/Dop:pump
//  Event/HWid:rgpio/Model:RASPBERRY/Dip:button/Value:High
 
// common fields
    
public String command;
public String hwid;

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
}
