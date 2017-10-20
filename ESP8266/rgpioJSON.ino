
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>

//String deviceType = "LEVEL";
String deviceType = "RELAY";
//String deviceType = "BUTTON";

/*

  The first line of this sketch identifies the device that this sketch should implement.
  To add a new type of device "MIXER" to this sketch, write the following functions (add at the end of the sketch)
  Also add the functions to the wrappes that are actually called in the code.
  The wrapper functions are called initDEVICE, setDEVICE, getDEVICE, eventDEVICE.
  Not all functions need to exist (a device XYZ without output pin has no setXYZ() function as it will never receive a Set command.
  If there are state variables that must be accessible to different methods, declare them with the functions. See example BUTTON.

  void initMIXER(){
    Is run in setup().
    For instance, pin modes are set here, and output pins are set to an initial value.
    initMIXER() must set the variable 'pinDescription' with the part of the Report string that describes the pins.
  }

  void setMIXER(String pinName, int state) {
    Is called when the device receives a Set command.
    The function executes the set command (e.g. DigitalWrite the GPIO pin corresponding to pinName)
  }

  String getMIXER(String pinName) {
    Is called when the device receives a Get command.
    The function returns the value of this pin as a String that can be sent back to the server (e.g. "High", "Low", "123")
  }

  void eventMIXER(){
    Is called as last statement of loop() and should check if an event took place.
    If so it should send the Event message to the server.
    This function can send multiple events if it has multiple input pins
  }


*/


int status = WL_IDLE_STATUS;

char ssid[] = "IoTwifi";
char pass[] = "verdiverdi";

unsigned int localPort = 2500;
unsigned int otherPort = 2600;

IPAddress serverIP;
IPAddress broadcastIP;

WiFiUDP UdpReceive;
WiFiUDP UdpSend;


void initDEVICE() {
  if (deviceType.equals("RELAY")) initRELAY();
  if (deviceType.equals("LEVEL")) initLEVEL();
  if (deviceType.equals("BUTTON")) initBUTTON();
}

String getDEVICE(String pinName) { // only devices with input pins
  if (deviceType.equals("BUTTON")) return getBUTTON(pinName);
  if (deviceType.equals("LEVEL")) return getLEVEL(pinName);
}

void setDEVICE(String pinName, String pinState) { // only devices with output pins
  if (deviceType.equals("RELAY")) setRELAY(pinName, pinState);
}

void eventDEVICE() { // only devices witch input pins
  if (deviceType == "BUTTON") eventBUTTON();
}



void setup() {

  Serial.begin(115200);

  initDEVICE();

  Serial.println();
  Serial.println("Connecting to:");
  Serial.println(ssid);
  Serial.println(pass);
  WiFi.begin(ssid, pass);
  Serial.print("Connecting");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  // sending to 255.255.255.255 does not seem to work
  // So we broadcast to the local broadcast address that we construct here
  // by OR-ing our IP address with the netmask

  broadcastIP = ~WiFi.subnetMask() | WiFi.localIP();
  Serial.print("broadcasting to ");
  Serial.println(broadcastIP);

  // start listening to the Udp Port

  UdpReceive.begin(localPort);

}  // end of setup()



boolean haveRemoteIP = false;
unsigned long lastTimeReportWasSent;
String pinDescription = "";  // set in initDEVICE, used in sendReport

void loop() {

  // As long as the server is not known, broadcast Report messages every second

  if (!haveRemoteIP) {
    if ((millis() - lastTimeReportWasSent) > 1000) {
      sendReport(broadcastIP);
      lastTimeReportWasSent = millis();
    }
  }

  char packetBuffer[255];

  int noBytes = UdpReceive.parsePacket();
  if (noBytes) {

    //clear packetBuffer before receiving a string from Udp
    for (int i = 0; i < 256; i++) {
      packetBuffer[i] = 0;
    }

    UdpReceive.read(packetBuffer, noBytes);
    String str(packetBuffer);
    Serial.print("Received string: ");
    Serial.println(str);

    // if we receive something on localPort, we assume it is from the RGPIO server.
    // From now on we know the server's IP address
    // (maybe we should ignore input if it is not a valid message)

    serverIP = UdpReceive.remoteIP();
    haveRemoteIP = true;

    if (str.startsWith("{") && str.endsWith("}")) {
      StaticJsonBuffer<500> jsonBuffer;
      JsonObject& root = jsonBuffer.parseObject(str);

      if (!root.success()) {
        Serial.println("parseObject() failed, input string :");
        Serial.println(str);
      } else {

        String destination = root["destination"];
        String command = root["command"];

        if (command == "REPORT") {
          sendReport(serverIP);
        }
        if (command == "GET") {
          String pinName = root["pin"];
          String pinValue;
          pinValue = getDEVICE(pinName);
          UdpSendString(UdpReceive.remotePort(), serverIP, pinValue);
        }
        if (command == "SET") {
          String pinName = root["pin"];
          String pinValue = root["value"];
          setDEVICE(pinName, pinValue);
          UdpSendString(UdpReceive.remotePort(), serverIP, "OK");
        }
      }
    }

    if (str.equals("OK")) { }  // The server sends OK to a REPORT so that the device can stop broadcasting

    UdpReceive.flush();
  }

  // Detect Events

  eventDEVICE();

} // end loop


// Utilities

// Procedure to send UPD packet

void UdpSendString (int poort, IPAddress destinationAddress, String message) {
  Serial.print("Sending string: ");
  Serial.println(message);
  UdpSend.beginPacket(destinationAddress, poort);
  UdpSend.print(message);
  UdpSend.endPacket();
  UdpSend.flush();
  UdpReceive.stop();   // stop and restart hack. Otherwise incorrect behaviour
  delay(10);
  UdpReceive.begin(localPort);
}

String stateToString(int state) {
  if (state == HIGH) return "High";
  if (state == LOW) return "Low";
  return "Unknown";
}

int stringToState(String state) {
  if (state == "High") return HIGH;
  else if (state == "Low") return LOW;
  else {
    Serial.print("stringToState : invalid input ");
    Serial.println(state);
    return 0;
  }
}

void sendReport(IPAddress ip) {
  String upTimeStr = String(millis() / 1000);
  String message = "Report/HWid:" + String(ESP.getChipId()) + "/Model:" + deviceType + "/Uptime:" + upTimeStr + pinDescription;
  UdpSendString(otherPort, ip, message);
}

// DEVICE methods

// initDEVICE must be implemented for all devices and at least set 'pinDescription'

void initRELAY() {
  pinMode(0, OUTPUT);
  digitalWrite(0, HIGH);
  pinDescription = "/Dop:0";
}

void initLEVEL() {
  pinMode(0, OUTPUT); //trigpin
  pinMode(3, INPUT); //echo pin
  pinDescription = "/Aip:level";
}

int buttonPrevious;  // keeps state of button so we can detect a change
void initBUTTON() {
  pinMode(2, INPUT);
  buttonPrevious = digitalRead(2);
  pinDescription = "/Dip:on";
}

// setDEVICE must be implemented for all devices that have output pins

void setRELAY(String pinName, String state) {
  Serial.print("Set dopName=");
  Serial.print(pinName);
  Serial.print(" state=");
  Serial.println(state);
  if (pinName == "0") {
    Serial.println("setting pin 0 (inverted)");
    if (state == "Low") digitalWrite(0, HIGH);
    if (state == "High") digitalWrite(0, LOW);
  }
  // if pinName is wrong we do nothing
}

// getDEVICE must be implemented for all devices that have input pins
// getDEVICE() should always return the string representation of the pin value (HIGH, LOW for Dip, number as string for Aip)

String getBUTTON(String pinName) {
  Serial.print("getBUTTON pinName=");
  Serial.print(pinName);
  if (pinName == "on") {
    Serial.println("getBUTTON reading pin 2");
    return stateToString(digitalRead(2));
  }
  // if dipName is wrong we return LOW
  return "Low";
}

String getLEVEL(String pinName) {
  Serial.print("getLEVEL pinName=");
  Serial.print(pinName);
  if (pinName == "level") {
    int duration;
    digitalWrite (0, LOW);
    delayMicroseconds(2);
    digitalWrite (0, HIGH);
    delayMicroseconds(10);
    digitalWrite (0, LOW);
    duration = pulseIn(3, HIGH);
    Serial.print("duration =");
    Serial.println(duration);
    return String(duration);
  }
  // if pinName is wrong we return 0
  String message = "getLEVEL wrong pin Name : <" + pinName + ">";
  UdpSendString(otherPort, serverIP, message);

  return "0";
}

// eventDEVICE must be implemented by all devices that can generate an event ( i.e. if they have Dip pins)

void eventBUTTON() {
  int buttonNow = digitalRead(2);
  if (buttonNow != buttonPrevious) {
    String message = "Event/HWid:" + String(ESP.getChipId()) + "/Model:BUTTON/Dip:on/Value:" + stateToString(buttonNow);
    UdpSendString(otherPort, serverIP, message);
    buttonPrevious = buttonNow;
  }
}

