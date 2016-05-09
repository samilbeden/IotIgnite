#include "DHT.h"
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>
#include <Timer.h>

#define DHTPIN 14
#define DHTTYPE DHT11   // DHT 11
#define HEARTBEAT_INTERVAL 5000
#define PORT 9999


DHT dht(DHTPIN, DHTTYPE);
int ledPin=4;
WiFiServer server(PORT);
WiFiClient client;
String response;

const char* ssid     = "Your-Wifi-SSID";
const char* password = "Your-Wifi-Password";

unsigned long readingFreq=10000;

Timer myDHTTimer;
Timer heartbeatTimer;
int myHeartbeatTimerInt;
int myDHTTimerInt;
unsigned int discovery_ttl=120;

String getMacAddress();
void readAllSensors();
String packetBuilder(String fahrenheit, String celcius, String humidity);
void connectToWifi();
void handleResponse();
void heartbeatGenerator();

void update();

void setup() {
  Serial.begin(115200);
  
  pinMode(ledPin,OUTPUT);
  dht.begin();

  connectToWifi();

  server.begin();
  server.setNoDelay(true);

  myDHTTimerInt = myDHTTimer.every(readingFreq,readAllSensors);
  myHeartbeatTimerInt = heartbeatTimer.every(HEARTBEAT_INTERVAL,heartbeatGenerator);
     while(true)
   {
     client = server.available();
     if(client.connected()){
       Serial.println("Client Connected!");  
       String macAdress = getMacAddress();
       client.write((uint8_t*) &macAdress[0], macAdress.length());
       break;
       
     }
   }
}

void loop() {

   if (client.connected()){
   
        
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


      handleResponse();
      myDHTTimer.update();
      heartbeatTimer.update();

    
  }else{
      while(true)
   {
     client = server.available();
     if(client.connected()){
       Serial.println("Client Connected!");  
       String macAdress = getMacAddress();
       client.write((uint8_t*) &macAdress[0], macAdress.length());
       break;
       
     }
   }
  }

  delay(100);
 
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

void connectToWifi(){
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    digitalWrite(ledPin,HIGH);
    delay(500);
    digitalWrite(ledPin,LOW);
    delay(500);
    Serial.print(".");
  }
 
  Serial.println("");
  Serial.println("WiFi connected");  
  Serial.println("IP address: ");
 
  IPAddress localIP = WiFi.localIP();
  Serial.println(localIP);
  
  String name = "esp8266 " + getMacAddress();
  if (!MDNS.begin(&name[0],localIP,discovery_ttl)) {
    Serial.println("Error setting up MDNS responder!");
    while(1) { 
      delay(1000);
    }
  }
  Serial.println("mDNS responder started");

  MDNS.addService("esp8266","tcp",PORT);
}

void handleResponse(){

   if(response.charAt(0)=='R'){
      String config = response.substring(12,response.length());
      Serial.print("Config : ");
      Serial.print(config);
      Serial.println();
      readingFreq = (unsigned long) config.toFloat();
      Serial.println("Read Freq Changed!");
      update();
   }else{
 
   if(response.equals("LED_ON\n")){
    Serial.println("Opening led...");
    digitalWrite(ledPin,HIGH);
    delay(100);
   }
   if(response.equals("LED_OFF\n")){
    Serial.println("Closing led...");
    digitalWrite(ledPin,LOW);
    delay(100);
     
   }
   }

   response="";
  }

  void update(){

    myDHTTimer.stop(myDHTTimerInt);

    myDHTTimerInt = myDHTTimer.every(readingFreq,readAllSensors);
  }

  void heartbeatGenerator(){

  if (WiFi.status() != WL_CONNECTED) {
   connectToWifi();
  }else{

    String macAdress = getMacAddress();
    client.write((uint8_t*) &macAdress[0], macAdress.length());
    String heartbeat = "[___-^-____-^-____-^____-^-____] ";
   /////////////------------Heartbeat MSG-------//////////////
   client.write((uint8_t*)&heartbeat[0], heartbeat.length());
   
  }
    
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
  


