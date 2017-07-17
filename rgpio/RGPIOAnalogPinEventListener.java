
package rgpio;

public interface RGPIOAnalogPinEventListener {
  
    void onAnalogPinEvent(RGPIOAnalogPinEvent event) throws Exception;   // exceptions from Thread.sleep()
 
}
