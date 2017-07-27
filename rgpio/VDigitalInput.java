

package rgpio;


public class VDigitalInput extends VInput {
    
    public boolean value(){
        if (super.get_value().equals("High")) return true;
         if (super.get_value().equals("Low")) return false; 
         return false;
    }

}
