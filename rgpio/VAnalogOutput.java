

package rgpio;


public class VAnalogOutput extends VOutput {
  
    public void setValue(float value){
        set(Float.toString(value));
    }

}
