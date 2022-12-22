#include <EthernetENC.h>
#include <MemoryUsage.h>

static byte mymac[] = { 0x70, 0x69, 0x69, 0x2D, 0x30, 0x31 };

static byte server[] = {192, 168, 254, 143};

//int button1 = 44;
//int button2 = 22;

int updatePeriod = 5000; //update every 5 seconds
unsigned long lastTime = 0; //represents last update time

struct Zone
{
  int id;
  int buttonPin;
  bool lastMessageClosed;
};

Zone zoneArr[] = {
  {1, 44, false},
  {2, 22, false},
  {3, 23, false},
  {4, 24, false},
  {5, 25, false},
  {6, 26, false}
};

const int zoneCount = sizeof(zoneArr)/sizeof(Zone);  

byte msgBuf[zoneCount*2]; 

//bool lastMessageClosed = false;  //represents whether the last message to the server was "open" or "closed". "open" is false. "closed" is true.

EthernetClient client;

void setup() {
 
  Serial.begin(57600);  

  for (int i = 0; i < zoneCount; i++)
  { 
    pinMode(zoneArr[i].buttonPin, INPUT_PULLUP);
  }
              
  //pinMode(button1, INPUT_PULLUP);
  //pinMode(button2, INPUT_PULLUP);
  Ethernet.begin(mymac);


  delay(1000);
  Ethernet.maintain();
  
  sendAllZones();
  //sendMessage(zoneCount*2);  //send whole buffer.
}

void loop () 
{
  Ethernet.maintain();
  //sendMessage();
  //FREERAM_PRINT;
  /*bool isClosed = digitalRead(button1) == LOW;
  //Serial.println(isOpen);
  if (isClosed && lastMessageClosed == false)  //PINS CONNECTED, MEANING ZONE CLOSED  
  {
    //Serial.println(F("Sending message"));
    sendMessage(1);
    lastMessageClosed = true;
  }
  else if (!isClosed && lastMessageClosed == true)  //PINS DISCONNECTED, MEANING ZONE OPEN
  {
    sendMessage(0);
    lastMessageClosed = false;
  }
*/
  int bufSize = 0;    //this keeps track of how many zones have been added to the buffer. we cannot use i here.
  for (int i = 0; i < zoneCount; i++) 
  { 
    Zone* zone = &zoneArr[i];
    bool isClosed = digitalRead(zone->buttonPin) == LOW;   
    /*Serial.print("Zone ");
    Serial.print(zone->id);
    Serial.print(": ");
    Serial.println(zone->lastMessageClosed);   */ 
    if ((isClosed && zone->lastMessageClosed == false) || (!isClosed && zone->lastMessageClosed == true))   //if (isClosed =! zone.lastMessageClosed)
    {
      msgBuf[(bufSize)] = zone->id;
      msgBuf[(bufSize)+1] = isClosed?1:0;
      zone->lastMessageClosed = isClosed;     
      bufSize+=2;       
    }
    /*else if ()  //PINS DISCONNECTED, MEANING ZONE OPEN
    {
      msgBuf[(bufSize*2)] = zone.id;
      msgBuf[(bufSize*2)+1] = isClosed?1:5;
      zone.lastMessageClosed = false;
    }*/
    
    
    //zone.lastMessageClosed = isClosed;
  }
  if (bufSize > 0)
  {
    sendMessage(bufSize);
  }
  else
  {
    //delay(100);
  }

  if((unsigned long)(millis() - lastTime) > updatePeriod)  //this protects against overflow issues when millis() gets to the max value. 
  {
    Serial.println("Keep alive");

    sendAllZones();
  }
}

void sendAllZones()
{
    for (int i = 0; i < zoneCount; i++)
    { 
      Zone* zone = &zoneArr[i];
      pinMode(zone->buttonPin, INPUT_PULLUP);
      bool isClosed = digitalRead(zone->buttonPin) == LOW;   

      msgBuf[(i*2)] = zone->id;         //here we are filling up the buffer with each zone. therefore, we can use i to iterate through both the zone array and the buffer
      msgBuf[(i*2)+1] = isClosed?1:0;
    
      zone->lastMessageClosed = isClosed;
    }
    lastTime = millis();  
    sendMessage(zoneCount*2);
}

void sendMessage(int msgLength)   //Uses the global msgBuf. Data is placed in there.
{
  if (client.connect(server, 3818))  
  {
    //client.print("Door Opened");    
    //byte msg[] = {1,state};
    //client.write(msg,2);
    client.write(msgBuf,msgLength);
    Serial.println(F("Message sent"));
  }
  else
  {
    Serial.println(F("Error connecting to the server. "));
  }
  client.stop();
}