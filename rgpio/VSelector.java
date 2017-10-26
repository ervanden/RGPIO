package rgpio;

import java.util.ArrayList;

public class VSelector {
    
    /* common methods for RGPIODevice and RGPIODigitalIn/Output groups
      A selector stores a regular expression based on which a device pin (Dip or Dop) is
       assigned to a RGPIODigitalInput or RGPIODigitalOutput.
       If the type is HWid and the device HWid matches the regex -> the device matches
       If the type is Model and the device model matches the regex -> the device matches
       If the Dip/Dop label matches the pinName AND the device matches -> the pin matches
    
       The Dip or Dop matches if one of the selectSpecList in the list matches.
    
       The same methods are also used to match a device to a RGPIODevice. Then the pinName is not used
    */ 
    
    public ArrayList<SelectSpec> selectSpecList = new ArrayList<>();

    class SelectSpec {

        String type;  // Model or HWid
        String regEx;
        String pinName = null;

        public SelectSpec(String pinName, String type, String regEx) {
            this.pinName = pinName;
            this.type = type;
            this.regEx = regEx;
        }
    }

    public void addSelectSpec(String pinName, String type, String regEx) {
 //         System.out.println("  ADD selector pin/type/regex " + pinName+"/"+type + "/" + regEx);

          selectSpecList.add(new SelectSpec(pinName, type, regEx));
 //                   System.out.println("  NR OF selectSpecList " + selectSpecList.size());
    }

    public boolean matchesDevicePin(String pinName, String model, String HWid) {
 //       System.out.println(" device matching to Model/HWid " + model + "/" + HWid);
 //                           System.out.println("-------------matchDevicePin model=" + model + " pin=" + pinName);
 //                           System.out.println("              nr selectSpecList=" + selectSpecList.size());    
        for (SelectSpec s : selectSpecList) {
 //                      System.out.println("  selector pin/type/regex " + selector.pinName+"/"+selector.type + "/" + selector.regEx);
            if (s.pinName.equals(pinName)) {
 //                     System.out.println("  Pin matches " + pinName);
                if (s.type.equals("Model")) {
 //                                      System.out.println("  TEST IF Model " + model + " matches " + selector.regEx);
                    if (model.matches(s.regEx)) {
 //                                              System.out.println("  Model " + model + " matches " + selector.regEx);
                        return true;
                    }
                }

                if (s.type.equals("HWid")) {
//                                       System.out.println("  TRY IF HWid " + HWid + " matches " + selector.regEx);
                    if (HWid.matches(s.regEx)) {
//                                               System.out.println("  HWid " + HWid + " matches " + selector.regEx);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean matchesDevice(String model, String HWid) {
 //      System.out.println(" device matching to Model/HWid " + model + "/" + HWid);

        for (SelectSpec s : selectSpecList) {

            if (s.type.equals("Model")) {
 //                  System.out.println("  TEST IF Model " + model + " matches " + s.regEx);
                if (model.matches(s.regEx)) {
 //                      System.out.println("  Model " + model + " matches " + s.regEx);
                    return true;
                }
            }

            if (s.type.equals("HWid")) {
 //                  System.out.println("  TRY IF HWid " + HWid + " matches " + s.regEx);
                if (HWid.matches(s.regEx)) {
//                      System.out.println("  HWid " + HWid + " matches " + s.regEx);
                    return true;
                }
            }
        }
        return false;
    }

}
