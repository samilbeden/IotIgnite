/**
 * @file OTA-mDNS-SPIFFS.ino
 * 
 * @author Pascal Gollor (http://www.pgollor.de/cms/)
 * @date 2015-09-18
 * 
 * changelog:
 * 2015-10-22: 
 * - Use new ArduinoOTA library.
 * - loadConfig function can handle different line endings
 * - remove mDNS studd. ArduinoOTA handle it.
 * 
 */

// includes
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>
#include <WiFiUdp.h>
#include <FS.h>
#include <Timer.h>

#include "DHT.h"


/**
 * @brief mDNS and OTA Constants
 * @{
 */
#define HOSTNAME "IotIgnite-ESP8266-" ///< Hostename. The setup function adds the Chip ID at the end.
#define PORT 9999
#define DHTPIN 5
#define DHTTYPE DHT11   // DHT 11
#define HEARTBEAT_INTERVAL 5000

unsigned long readingFreq=10000;


DHT dht(DHTPIN, DHTTYPE);
Timer myDHTTimer;
Timer heartbeatTimer;
int myHeartbeatTimerInt;
int myDHTTimerInt;
int ledPin = 13;
int confPin = 14;
unsigned int discovery_ttl=120;
String configuredNodeID=".";
/// @}

/**
 * @brief Default WiFi connection information.
 * @{
 */
/// @}

/// Uncomment the next line for verbose output over UART.
//#define SERIAL_VERBOSE

/**
 * @brief Read WiFi connection information from file system.
 * @param ssid String pointer for storing SSID.
 * @param pass String pointer for storing PSK.
 * @return True or False.
 * 
 * The config file have to containt the WiFi SSID in the first line
 * and the WiFi PSK in the second line.
 * Line seperator can be \r\n (CR LF) \r or \n.
 */
bool loadConfig(String *ssid, String *pass)
{
  // open file for reading.
  File configFile = SPIFFS.open("/cl_conf.txt", "r");
  if (!configFile)
  {
    Serial.println("Failed to open cl_conf.txt.");

    return false;
  }

  // Read content from config file.
  String content = configFile.readString();
  configFile.close();
  
  content.trim();

  // Check if ther is a second line available.
  int8_t pos = content.indexOf("\r\n");
  uint8_t le = 2;
  // check for linux and mac line ending.
  if (pos == -1)
  {
    le = 1;
    pos = content.indexOf("\n");
    if (pos == -1)
    {
      pos = content.indexOf("\r");
    }
  }

  // If there is no second line: Some information is missing.
  if (pos == -1)
  {
    Serial.println("Infvalid content.");
    Serial.println(content);

    return false;
  }

  // Store SSID and PSK into string vars.
  *ssid = content.substring(0, pos);
  *pass = content.substring(pos + le);

  ssid->trim();
  pass->trim();

#ifdef SERIAL_VERBOSE
  Serial.println("----- file content -----");
  Serial.println(content);
  Serial.println("----- file content -----");
  Serial.println("ssid: " + *ssid);
  Serial.println("psk:  " + *pass);
#endif

  return true;
} // loadConfig

bool loadNodeID(String *nodeID){
    // open file for reading.
  File configFile = SPIFFS.open("/node_conf.txt", "r");
  if (!configFile)
  {
    Serial.println("Failed to open node_conf.txt.");

    return false;
  }

  String content = configFile.readString();
  configFile.close();


  content.trim();

  *nodeID = content;
  nodeID->trim();

      Serial.println("Loading nodeID");
      Serial.println("nodeID: " + *nodeID);


  return true;
}
/**
 * @brief Save WiFi SSID and PSK to configuration file.
 * @param ssid SSID as string pointer.
 * @param pass PSK as string pointer,
 * @return True or False.
 */
bool saveConfig(String *ssid, String *pass)
{
  // Open config file for writing.
  File configFile = SPIFFS.open("/cl_conf.txt", "w");
  if (!configFile)
  {
    Serial.println("Failed to open cl_conf.txt for writing");

    return false;
  }

  // Save SSID and PSK.
  configFile.println(*ssid);
  configFile.println(*pass);

  configFile.close();
  
  return true;
} // saveConfig

bool saveNodeID(String *nodeID){
  // Open config file for writing.
  File configFile = SPIFFS.open("/node_conf.txt", "w");
  if (!configFile)
  {
    Serial.println("Failed to open node_conf.txt for writing");

    return false;
  }

  // Save nodeID

   
   
  configFile.println(*nodeID);
  configFile.close();

    Serial.println("Saving nodeID");
    Serial.println("nodeID: " + *nodeID);
  
  return true;
}
String station_ssid ="";
String station_psk = "";
/**
 * @brief Arduino setup function.
 */

 
WiFiServer server(PORT);
WiFiClient client;

String getMacAddress();
void doAPModeOperations();
void handleConfigurationResponse();

void handleDataResponse();
void heartbeatGenerator();
void readAllSensors();
void startZeroConfService();
String packetBuilder(String fahrenheit, String celcius, String humidity);
void updateTimers();
void sendNodeID();

void resetConfigurations();
void setup()
{

  Serial.begin(115200);
  
  delay(100);

  Serial.println("\r\n");
  Serial.print("Chip ID: 0x");
  Serial.println(ESP.getChipId(), HEX);

  // Set Hostname.
  String hostname(HOSTNAME);
  hostname += String(ESP.getChipId(), HEX);
  WiFi.hostname(hostname);

  // Print hostname.
  Serial.println("Hostname: " + hostname);
  //Serial.println(WiFi.hostname());


  // Initialize file system.
  if (!SPIFFS.begin())
  {
    Serial.println("Failed to mount file system");
    return;
  }

  // Load wifi connection information.
  if (! loadConfig(&station_ssid, &station_psk))
  {
    station_ssid = "";
    station_psk = "";

    Serial.println("No WiFi connection information available.");
  }

  // Check WiFi connection
  // ... check mode
  if (WiFi.getMode() != WIFI_STA)
  {
    WiFi.mode(WIFI_STA);
    delay(10);
  }

  // ... Compare file config with sdk config.
  if (WiFi.SSID() != station_ssid || WiFi.psk() != station_psk)
  {
    Serial.println("WiFi config changed.");

    // ... Try to connect to WiFi station.
    WiFi.begin(station_ssid.c_str(), station_psk.c_str());

    // ... Pritn new SSID
    Serial.print("new SSID: ");
    Serial.println(WiFi.SSID());
    loadNodeID(&configuredNodeID);
    Serial.println("new Node ID :");
    Serial.println(configuredNodeID);


    // ... Uncomment this for debugging output.
    //WiFi.printDiag(Serial);
  }
  else
  {
    // ... Begin with sdk config.
    WiFi.begin();
  }

  Serial.println("Wait for WiFi connection.");

  // ... Give ESP 10 seconds to connect to station.
  unsigned long startTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startTime < 10000)
  {
    Serial.write('.');
    //Serial.print(WiFi.status());
    delay(500);
  }
  Serial.println();

  // Check connection
  if(WiFi.status() == WL_CONNECTED)
  {
    // ... print IP Address
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    startZeroConfService();
  }
  else
  {
    Serial.println("Can not connect to WiFi station. Go into AP mode.");
    
    // Go into software AP mode.
    WiFi.mode(WIFI_AP);

    delay(10);
    int bufflength = hostname.length()+1;
    char charSSIDBuffer[bufflength];
    hostname.toCharArray(charSSIDBuffer,bufflength);
    WiFi.softAP(charSSIDBuffer);

    Serial.print("IP address: ");
    Serial.println(WiFi.softAPIP());
  }

   pinMode(ledPin,OUTPUT);
   pinMode(confPin,OUTPUT); 
     server.begin();
     server.setNoDelay(true);

     dht.begin();


    // String app = "..";

   //  saveConfig(&app,&app);
    // saveNodeID(&app);
}


/**
 * @brief Arduino loop function.
 */






String response;

void loop()
{

 if (WiFi.getMode() == WIFI_AP){
    doAPModeOperations();
 }else {

    // Wifi in station mode. //
    // Start NSD and normal code here. //
    Serial.println("Connected to wifi network.!!!!!!!");
    Serial.println(WiFi.SSID());
    digitalWrite(ledPin, HIGH);  // Turn the LED off by making the voltage HIGH
     if (client.connected()){
       Serial.println("Client is ready.");
        
        while(client.available()){
        char c =client.read();
          response += c;
      }
     
      client.flush();
      if(!response.equals("")){
      Serial.print("Data : ");
      Serial.print(response);
      Serial.println();
      }


      handleDataResponse();
      myDHTTimer.update();
      heartbeatTimer.update();

    
  }else{
      while(true)
   {
     digitalWrite(ledPin,HIGH);
     delay(100);
     digitalWrite(ledPin,LOW);
     client = server.available();
     if(client.connected()){
       Serial.println("Client Connected! Sending NODEID");
             sendNodeID();
      
       digitalWrite(ledPin,HIGH);  

       delay(1000);
       break;
       
     }else{
        Serial.println("Client is NOT Connected!");
      }

     delay(100);
   }
  }

  delay(100);


  
 } // end of WIFI_STATION_MODE


  delay(10);

}



String getMacAddress() {
    
    byte mac[6];

    WiFi.macAddress(mac);
    String cMac = "~";
      for (int i = 0; i < 6; ++i) {
        cMac += String(mac[i],HEX);
            if(i<5)
              cMac += ":";

      }

    cMac.toUpperCase();
    cMac+="~";
    return cMac;
}


void doAPModeOperations(){

  if(!client.connected()){
     Serial.println("Esp in AP mode... Waiting for configurations.....");

     
     
        while(true){
                
          client = server.available();
                              
                              if(client.connected()){
                                    Serial.println("Client Connected!");  
       
                                    String macAdress = getMacAddress();
                                    client.write((uint8_t*) &macAdress[0], macAdress.length());
                              break;
       
                              }else{
        
                                     Serial.println("Waiting for client...");

                                     digitalWrite(ledPin, LOW);   // Turn the LED on (Note that LOW is the voltage level
                                     // Wait for a second
                                     delay(3000);
                                     digitalWrite(ledPin, HIGH);  // Turn the LED off by making the voltage HIGH
     
                                     delay(3000);
                              }
      }
  }else{
    Serial.println("Client connected waiting for configurations....!!");
     while(client.available()){
        char c =client.read();
          response += c;
      }

      client.flush();
      if(!response.equals("")){
      Serial.print("Configuration Message Arrived : ");
      Serial.print(response);
      Serial.println();

      handleConfigurationResponse();
      }
  }


  delay(3000);
  
}

void handleConfigurationResponse(){

const int numberOfPieces = 3;
int counter = 0;
int lastIndex = 0;

String parsedStrings[numberOfPieces];

      for (int i = 0; i < response.length(); i++) {
        // Loop through each character and check if it's a comma
        if (response.substring(i, i+1) == "~") {
          // Grab the piece from the last index up to the current position and store it
          parsedStrings[counter] = response.substring(lastIndex, i);
          // Update the last position and add 1, so it starts from the next character
          lastIndex = i + 1;
          // Increase the position in the array that we store into
          counter++;
        }

        // If we're at the end of the string (no more commas to stop us)
        if (i == response.length() - 1) {
          // Grab the last part of the string from the lastIndex to the end
          parsedStrings[counter] = response.substring(lastIndex, i);
        }
      }

  Serial.println("SSID:  ");
  Serial.println(parsedStrings[0]);

    Serial.println("Password:  ");
  Serial.println(parsedStrings[1]);

    Serial.println("Node ID:  ");
  Serial.println(parsedStrings[2]);

  response="";

  saveConfig(&parsedStrings[0],&parsedStrings[1]);

  saveNodeID(&parsedStrings[2]);

  ESP.restart();
}

void readAllSensors(){
  String packet="";
  float h = dht.readHumidity();
  // Read temperature as Celsius
  float t = dht.readTemperature();
  // Read temperature as Fahrenheit
  float f = dht.readTemperature(true);
  
  // Check if any reads failed and exit early (to try again).
  if (isnan(h) || isnan(t) || isnan(f)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }
  
  String f_String = String(f);
  String c_String =String(t);
  String h_String =String(h);
  // Compute heat index
  // Must send in temp in Fahrenheit!
  float hi = dht.computeHeatIndex(f, h);
  
  packet = packetBuilder(f_String,c_String,h_String);
  /////////////------------WIRELESS-------//////////////
   client.write((uint8_t*)&packet[0], packet.length());

 // Serial1.print("Humidity: "+ h);
  Serial.print('#');
  Serial.print("Humidity: ");
  Serial.print(h);
  Serial.print("  Temperature Celsius: ");
  Serial.print(t);
  Serial.print(" *C");
  Serial.println();
  Serial.println("Packet : " +packet);
  
  packet="";
  delay(100);

}

String packetBuilder(String fahrenheit, String celcius, String humidity)
{ String packet="";

  packet +="#";
  packet +="|Checksum|+";
  
  packet +="DHT11:|"+fahrenheit+"|"+celcius+"|"+humidity+"|";
  
  packet +="~";

  return packet;
}

void startZeroConfService(){
  
  String name = "esp8266 " + getMacAddress();
  IPAddress localIP = WiFi.localIP();
  if (!MDNS.begin(&name[0],localIP,discovery_ttl)) {
    Serial.println("Error setting up MDNS responder!");
    while(1) { 
      delay(1000);
    }
  }
  Serial.println("mDNS responder started");

  MDNS.addService("esp8266","tcp",PORT);
}

void updateTimers(){

    myDHTTimer.stop(myDHTTimerInt);
    myDHTTimerInt = myDHTTimer.every(readingFreq,readAllSensors);

    heartbeatTimer.stop(myHeartbeatTimerInt);
    myHeartbeatTimerInt = heartbeatTimer.every(HEARTBEAT_INTERVAL,heartbeatGenerator);

  }

  void heartbeatGenerator(){

  if (WiFi.status() != WL_CONNECTED) {
   setup();;
  }else{

    String macAdress = getMacAddress();
    client.write((uint8_t*) &macAdress[0], macAdress.length());
    String heartbeat = "[___-^-____-^-____-^____-^-____] ";
    /////////////------------Heartbeat MSG-------//////////////
    client.write((uint8_t*)&heartbeat[0], heartbeat.length());
   
  }
    
  }

  
  void sendNodeID(){

  if (WiFi.status() != WL_CONNECTED) {
   setup();
  }else{
      if(configuredNodeID.length() > 1){
            Serial.println("ID");
               Serial.println(configuredNodeID);

      String packet="";
      packet+="&";
      packet+=configuredNodeID;
      packet+="&";

      Serial.println("Sending NODE Packet");
      Serial.println(packet);
      client.write((uint8_t*) &packet[0], packet.length());

      packet="";
      }
   
  }
    
  }

  void handleDataResponse(){

   if(response.charAt(0)=='R'){
      String config = response.substring(12,response.length());
      Serial.print("Config : ");
      Serial.print(config);
      Serial.println();
      readingFreq = (unsigned long) config.toFloat();
      Serial.println("Read Freq Changed!");
      updateTimers();
   }else{
 
   if(response.equals("LED_ON\n")){
    Serial.println("Opening led...");
    digitalWrite(confPin,HIGH);
    delay(100);
   }
   if(response.equals("LED_OFF\n")){
    Serial.println("Closing led...");
    digitalWrite(confPin,LOW);
    delay(100);
     
   }
   }

   response="";
  }

  void resetConfigurations(){

    String pp=".....";
    String uu = ".....";
    String nodeID="";
    saveConfig(&pp,&uu);
    saveNodeID(&nodeID);

    ESP.restart();
  }




