package rgpio;

import utils.JSONString;

public class VAnalogInput extends VInput {

    private String integerToString(Integer i) {
        if (i == null) {
            return "NaN";
        } else {
            return i.toString();
        }
    }

    public String toJSON() {
        JSONString json = new JSONString();
        json.addProperty("object", "VIO");
        json.addProperty("name", name);
        json.addProperty("members", members.toString());
        json.addProperty("type", type.name());

        json.addProperty("avg", integerToString(avg()));
        json.addProperty("min", integerToString(min()));
        json.addProperty("max", integerToString(max()));

        return json.asString();
    }

    // for avg(), min(), max()   Integer.MIN_VALUE or MAXVALUE mean : NaN
    public Integer avg() {
        float sum = 0;
        int n = 0;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (!dip.value.equals("NaN")) { // the string "NaN" does not throw exception !
                            try {
                                float f = Float.parseFloat(dip.value);
                                sum = sum + f;
                                n = n + 1;
                            } catch (NumberFormatException nfe) {
                            }
                        }
                    }
                }
            }
        }
        if (n > 0) {
            return Math.round(sum / n);
        } else {
            return null;
        }
    }

    public Integer min() {
        Integer result = Integer.MAX_VALUE;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (!dip.value.equals("NaN")) { // the string "NaN" does not throw exception ?
                            try {
                                int f = Integer.parseInt(dip.value);
                                if (f < result) {
                                    result = f;
                                }
                            } catch (NumberFormatException nfe) {
                            }
                        }
                    }
                }
            }
        }
        if (result < Integer.MAX_VALUE) {
            return result;
        } else {
            return null;
        }

    }

    public Integer max() {
        Integer result = Integer.MIN_VALUE;
        for (PDevice device : RGPIO.PDeviceMap.values()) {
            for (PInput dip : device.inputs.values()) {
                if (dip.vinput == this) {
                    if (dip.value != null) { // is null before first GET or EVENT
                        if (!dip.value.equals("NaN")) { // the string "NaN" does not throw exception ?
                            try {
                                int f = Integer.parseInt(dip.value);
                                if (f > result) {
                                    result = f;
                                }
                            } catch (NumberFormatException nfe) {
                            }
                        }
                    }
                }
            }
        }
        if (result > Integer.MIN_VALUE) {
            return result;
        } else {
            return null;
        }
    }
}
