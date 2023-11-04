#include <EthernetENC.h>
#include <MemoryUsage.h>

static byte mymac[] = { 0x70, 0x69, 0x69, 0x2D, 0x30, 0x31 };

static byte serverIp[] = {192, 168, 254, 143};

EthernetClient client;
EthernetServer server = EthernetServer(21); //use port 21 for arduino server

static byte magicPacket[] = {0x6a, 0x38, 0x65, 0xe0, 0x1c, 0xab, 0x83}; // 0x6a3865e01cab83 - basically the password for garage door open from tcp.

byte recieveBuffer[8]; //8 byte recieve buffer

int updatePeriod = 2; //update every 2 seconds
unsigned long lastTime = 0; //represents last update time

struct Zone
{
  int id;
  int buttonPin;
  bool lastMessageClosed;
};

struct ZoneAction
{
  int id;
  bool (*function)(EthernetClient); // these should be fast, non-blocking function. no sleep in here.
};

Zone zoneArr[] = {
  {1, 44, false},
  {2, 22, false},
  {3, 23, false}, //garage door
  {4, 24, false},
  {5, 25, false},
  {6, 26, false}
};

bool openGarageDoor(EthernetClient client); // defined towards the bottom of the file

ZoneAction zoneActionArr[] = {
  {3, &openGarageDoor}  
};

//static int garageDoorId = 3;

const int zoneCount = sizeof(zoneArr)/sizeof(Zone);  
const int zoneActionCount = sizeof(zoneActionArr)/sizeof(ZoneAction);

byte msgBuf[zoneCount*2]; 

//bool lastMessageClosed = false;  //represents whether the last message to the server was "open" or "closed". "open" is false. "closed" is true.

void setup() 
{
  Serial.begin(57600);  

  for (int i = 0; i < zoneCount; i++)
  { 
    pinMode(zoneArr[i].buttonPin, INPUT_PULLUP);
  }
  
  pinMode(41, OUTPUT);
  
  Serial.println("Ethernet.begin()....");
  Ethernet.begin(mymac);

  delay(1000);
  Ethernet.maintain();
  
  server.begin();
  sendAllZones();
  //sendMessage(zoneCount*2);  //send whole buffer.
}

void loop () 
{
  Ethernet.maintain();
  
  // if an incoming client connects, there will be bytes available to read:
  EthernetClient client = server.available();
  if (client == true) 
  {
    while (client.available() >= 8)  // opening garage door requires an 8 byte packet. read all 8 byte packets
    {
      for (int i = 0; i < 8; i++)
      {
        recieveBuffer[i] = client.read();
      }
      if (memcmp(recieveBuffer, magicPacket, 7) == 0)
      {
        Serial.print("RECIEVED MAGIC PACKET FOR ZONE "); 
        Serial.println(recieveBuffer[7]);
        bool found = false;
        for (int i = 0; i < zoneActionCount; i++)
        {
          if (zoneActionArr[i].id == recieveBuffer[7]) // if this element is the zone in question
          {
            found = true;
            bool result = (*zoneActionArr[i].function)(client); // call the function that corresponds to the action
            if (result == true)
              client.write("success");
            else
            {
              char msg[] = "bad request for zone id:  ";// + String(recieveBuffer[7]);
              msg[sizeof(msg)-2]=recieveBuffer[7]+48; // null terminated string. change the second to last byte.
              client.write(msg);
            }
            break;            
          }
        }
        if (!found)
        {
          char msg[] = "no zone action for zone id:  ";// + String(recieveBuffer[7]);
          msg[sizeof(msg)-2]=recieveBuffer[7]+48; // null terminated string. change the second to last byte. 48 is 0 in ascii.
          client.write(msg);      
        }
        
        /*if (recieveBuffer[7] == garageDoorId)
        {
          client.write("success");
          digitalWrite(41,HIGH);
          delay(500);
          digitalWrite(41,LOW);
        }
        else
        {
          client.write("bad request");
        }*/
      }
      else
      {
        client.write("rejected");        
      }      
    }
    while (client.available()) // dump any remaining data that doesnt fit 8
    {
      client.read();
    }
    client.flush();
    client.stop();    
    //client.write(client.read());
  }

  int bufSize = 0;    //this keeps track of how many zones have been added to the buffer. we cannot use i here.
  for (int i = 0; i < zoneCount; i++) 
  { 
    Zone* zone = &zoneArr[i];
    bool isClosed = digitalRead(zone->buttonPin) == LOW;   

    if (isClosed != zone->lastMessageClosed)//(isClosed && zone->lastMessageClosed == false) || (!isClosed && zone->lastMessageClosed == true))   //if (isClosed =! zone.lastMessageClosed)
    {
      msgBuf[(bufSize)] = zone->id;
      msgBuf[(bufSize)+1] = isClosed?1:0;
      zone->lastMessageClosed = isClosed;     
      bufSize+=2;       
    }
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
  
  //if ()
  checkTimeouts();
  //FREERAM_PRINT
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
  if (client.connect(serverIp, 3818))  
  {
    client.write(msgBuf,msgLength);
    Serial.println(F("Message sent"));
  }
  else
  {
    Serial.println(F("Error connecting to the server. "));
  }
  client.stop();
}

bool doorTimeoutLock = false;
void garageDoorTimeoutCallback()
{
  digitalWrite(41,LOW);
  doorTimeoutLock = false;
}

bool openGarageDoor(EthernetClient client)
{
  if (!doorTimeoutLock)  // timeout lock to make sure that another open event isn't started while the other was clicked already. 
  {
    digitalWrite(41,HIGH);
    setTimeout(&garageDoorTimeoutCallback, 300); // simulate pressing the garage door button for ___ milliseconds
    doorTimeoutLock = true;
    return true;
  }
  return false;
  //client.write("success");
}