package rgpio;

public interface RGPIOTimerListener {

    // Called when the timer with this name timed out.
    
    void onTimeOut(String timerName) throws Exception;

}
