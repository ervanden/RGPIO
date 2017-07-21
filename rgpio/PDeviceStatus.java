package rgpio;

public enum PDeviceStatus {
    NULL,
    /* not yet set (avoid nullpointer exception) */
    ACTIVE,
    /* ACTIVE
         device is set to ACTIVE when RGPIO receives
          - OK reply to a command
          - Report
          - Event
     */
    NOTRESPONDING,
    /*
       device HWid and deviceGroup name are known but did not yet respond to ALIVE requests
     */
    UNASSIGNED;
    /* UNASSIGNED
          Device is ALIVE but no deviceGroup name is assigned to it yet
          HWid is known
          deviceGroup name is not known
          This state may be caused by
           - first power-up of the device, deviceGroup name to be entered by the user
           - power-up of the device while the application is running.
             If the HWid matches an UNKNOWN device, this goes to the ACTIVE state
             Otherwise the device may be part of a group of devices with the same deviceGroup name (garden lights).
          If a device gets the UNASSIGNED state, this triggers an RPGIO event "unassignedDevice"
     */

}
