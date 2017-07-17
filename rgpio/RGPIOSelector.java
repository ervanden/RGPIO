package rgpio;

import java.util.ArrayList;

public class RGPIOSelector {
    
    /* common methods for RGPIODevice and RGPIODigitalIn/Output groups
      A selector stores a regular expression based on which a device pin (Dip or Dop) is
       assigned to a RGPIODigitalInput or RGPIODigitalOutput.
       If the type is HWid and the device HWid matches the regex -> the device matches
       If the type is Model and the device model matches the regex -> the device matches
       If the Dip/Dop label matches the pinName AND the device matches -> the pin matches
    
       The Dip or Dop matches if one of the selectors in the list matches.
    
       The same methods are also used to match a device to a RGPIODevice. Then the pinName is not used
    */ 
    
    public ArrayList<Selector> selectors = new ArrayList<>();

    class Selector {

        String type;  // Model or HWid
        String regEx;
        String pinName = null;

        public Selector(String pinName, String type, String regEx) {
            this.pinName = pinName;
            this.type = type;
            this.regEx = regEx;
        }
    }

    public void addSelector(String pinName, String type, String regEx) {
 //         System.out.println("  ADD selector pin/type/regex " + pinName+"/"+type + "/" + regEx);

          selectors.add(new Selector(pinName, type, regEx));
 //                   System.out.println("  NR OF selectors " + selectors.size());
    }

    public boolean matchesDevicePin(String pinName, String model, String HWid) {
 //       System.out.println(" device matching to Model/HWid " + model + "/" + HWid);
 //                           System.out.println("-------------matchDevicePin model=" + model + " pin=" + pinName);
 //                           System.out.println("              nr selectors=" + selectors.size());    
        for (Selector selector : selectors) {
 //                      System.out.println("  selector pin/type/regex " + selector.pinName+"/"+selector.type + "/" + selector.regEx);
            if (selector.pinName.equals(pinName)) {
 //                     System.out.println("  Pin matches " + pinName);
                if (selector.type.equals("Model")) {
 //                                      System.out.println("  TEST IF Model " + model + " matches " + selector.regEx);
                    if (model.matches(selector.regEx)) {
 //                                              System.out.println("  Model " + model + " matches " + selector.regEx);
                        return true;
                    }
                }

                if (selector.type.equals("HWid")) {
//                                       System.out.println("  TRY IF HWid " + HWid + " matches " + selector.regEx);
                    if (HWid.matches(selector.regEx)) {
//                                               System.out.println("  HWid " + HWid + " matches " + selector.regEx);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean matchesDevice(String model, String HWid) {
//       System.out.println(" device matching to Model/HWid " + model + "/" + HWid);

        for (Selector selector : selectors) {

            if (selector.type.equals("Model")) {
                //                   System.out.println("  TEST IF Model " + model + " matches " + selector.regEx);
                if (model.matches(selector.regEx)) {
                    //                       System.out.println("  Model " + model + " matches " + selector.regEx);
                    return true;
                }
            }

            if (selector.type.equals("HWid")) {
                //                   System.out.println("  TRY IF HWid " + HWid + " matches " + selector.regEx);
                if (HWid.matches(selector.regEx)) {
                    //                       System.out.println("  HWid " + HWid + " matches " + selector.regEx);
                    return true;
                }
            }
        }
        return false;
    }

}
