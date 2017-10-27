
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>

//String deviceType = "LEVEL";
//String deviceType = "RELAY";
String deviceType = "BUTTON";

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

// wrapper functions that call the device methods.
// For each new device that is added to this sketch, the wrapper functions have to be edited
// if there is a corresponding device method.(not all devices have all the methods)

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
  // by OR-ing our own IP address with the netmask

  broadcastIP = ~WiFi.subnetMask() | WiFi.localIP();
  Serial.print("broadcasting to ");
  Serial.println(broadcastIP);

  // start listening to the Udp Port

  UdpReceive.begin(localPort);


}  // end of setup()



boolean haveRemoteIP = false;
unsigned long lastTimeReportWasSent;


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

// utilities to convert digital state between String "High"/"Low" and int HIGH/LOW

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

// functions addDip(), addDop(), addAip(), addAop() to be used in initDEVICE()

String dips = "";
String dipsSeparator = "";
String dops = "";
String dopsSeparator = "";
String aips = "";
String aipsSeparator = "";
String aops = "";
String aopsSeparator = "";

void addDip(String name) {
  dips = dips + dipsSeparator + "\"" + name + "\"";
  dipsSeparator = ",";
}
void addDop(String name) {
  dops = dops + dopsSeparator + "\"" + name + "\"";
  dopsSeparator = ",";
}
void addAip(String name) {
  aips = aips + aipsSeparator + "\"" + name + "\"";
  aipsSeparator = ",";
}
void addAop(String name) {
  aops = aops + aopsSeparator + "\"" + name + "\"";
  aopsSeparator = ",";
}

// function to construct a JSON string (used to create Report and Event messages)


String jsonString;
String propertySeparator;

void startJson() {
  jsonString = "{";
  propertySeparator = "";
}
void addToJson(String name, String value) {
  jsonString = jsonString + propertySeparator + "\"" + name + "\":\"" + value + "\"";
  propertySeparator = ",";
}
void addArrayToJson(String name, String value) {
  jsonString = jsonString + propertySeparator + "\"" + name + "\":[" + value + "]";
  propertySeparator = ",";
}
String endJson() {
  jsonString = jsonString + "}";
}

// construct a Report message and send to the server port on this IPAddress

void sendReport(IPAddress ip) {
  String upTimeStr = String(millis() / 1000);

  startJson();
  addToJson("command", "REPORT");
  addToJson("hwid", String(ESP.getChipId()));
  addToJson("model", deviceType);
  addToJson("uptime", upTimeStr);
  if (dips != "") addArrayToJson("dips", dips);
  if (dops != "") addArrayToJson("dops", dops);
  if (aips != "") addArrayToJson("aips", aips);
  if (aops != "") addArrayToJson("aops", aops);
  endJson();
  Serial.println("json=" + jsonString);
  UdpSendString(otherPort, ip, jsonString);
}

// construct an Event message and send to the server port on this IPAddress

void sendEvent(String pinName, String pinValue) {

  startJson();
  addToJson("command", "EVENT");
  addToJson("hwid", String(ESP.getChipId()));
  addToJson("model", deviceType);
  addToJson("pin", pinName);
  addToJson("value", pinValue);
  endJson();
  Serial.println("json=" + jsonString);
  UdpSendString(otherPort, serverIP, jsonString);
}


// DEVICE methods

// initDEVICE must be implemented for all devices (since the device pins are defined here)
void initRELAY() {
  pinMode(0, OUTPUT);
  digitalWrite(0, HIGH);
  addDop("0");
}

void initLEVEL() {
  pinMode(0, OUTPUT); //trigpin
  pinMode(3, INPUT); //echo pin
  addAip("level");
}

int buttonPrevious0;  // keeps state of button so we can detect a change
int buttonPrevious2;
void initBUTTON() {
  pinMode(0, INPUT);
  pinMode(2, INPUT);
  buttonPrevious0 = digitalRead(0);
  buttonPrevious2 = digitalRead(2);
  addDip("on0");
  addDip("on2");
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
  if (pinName == "on0") {
    Serial.println("getBUTTON reading pin 0");
    return stateToString(digitalRead(0));
  }
  if (pinName == "on2") {
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
  int buttonNow = digitalRead(0);
  if (buttonNow != buttonPrevious0) {
    sendEvent("on0", stateToString(buttonNow));
    buttonPrevious0 = buttonNow;
  }
  buttonNow = digitalRead(2);
  if (buttonNow != buttonPrevious2) {
    sendEvent("on2", stateToString(buttonNow));
    buttonPrevious2 = buttonNow;
  }
}

