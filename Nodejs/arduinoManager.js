const { time } = require('console');
var net = require('net');
const server = net.createServer(onClientConnection);
const sqlManager = require('./sqlManager.js');
const androidDeviceManager = require('./androidDeviceManager.js');
const speakerManager = require('./speakerManager/speakerManager.js');
const loggingManager = require('./loggingManager.js');


//var _recievedEventCallback;

exports.zones = [];


class Zone
{
    static OFFLINE = -1;
    static OPEN = 0;
    static CLOSED = 1;
    static MAGICPACKET = [0x6a, 0x38, 0x65, 0xe0, 0x1c, 0xab, 0x83];
    constructor(id, name, muted, state, userControl)
    {
        this.id = id;
        this.name = name;
        this.muted = muted;
        this.state = state;
        this.timeStamp = new Date();
        this.userControl = userControl;
        this.isAlive = false;
        this.ipAddress;
    }

/*    getJson()
    {

        return JSON.stringify({id: this.id, name: this.name, });
    } */
}

const timer = setInterval(function keepAlive()
{
    var zonesUpdated = [];
    exports.zones.forEach(function each(zone)
    {
        if (zone.isAlive === false)
        {
            if (zone.state != -1) //if it was not previously offline
            {
                zone.state = -1;  //set it to now be offline
                zonesUpdated.push(zone); 
            }
            return;  //equivalent of continue
        }

        zone.isAlive = false;
    });
    if (zonesUpdated.length > 0)
        androidDeviceManager.informDevices(zonesUpdated); //inform the devices that the zone(s) went offline

}, 7500);  //7.5 seconds. Arduino should send every 5. 

sqlManager.makeQuery("SELECT * FROM dbo.Zones", data =>
{
    data.recordset.forEach(entry =>
    {
        exports.zones.push(new Zone(entry.id, entry.name, entry.muted, -1, entry.userControl));
    });
    //exports.userDevices = data.recordSet
    exports.startListening();
});

exports.startListening = function ()
{
    //_recievedEventCallback = recievedEventCallback;
    var port = 3818;
    server.listen(port, function ()
    {
        console.log(`Arduino server started on port ${port}`);
    });
}

exports.zoneAction = function (id, callback)
{
    var zoneIndex = exports.zones.findIndex(o => o.id == id);
    if (zoneIndex == -1){
        console.log(`ZONE ${id} DOES NOT EXIST`);
        callback(`ZONE ${id} DOES NOT EXIST`);
        return;
    }
    else if (exports.zones[zoneIndex].state == Zone.OFFLINE)
    {
        console.log(`ZONE ${id} IS OFFLINE`);
        callback(`ZONE ${id} IS OFFLINE`);
        return;
    }
    
    var client = new net.Socket();
    client.connect(21, exports.zones[zoneIndex].ipAddress, function() 
    {
        console.log('CONNECTED TO' + exports.zones[zoneIndex].ipAddress + ':' + 21);
        var bytesToSend = [];
        bytesToSend = Array.from(Zone.MAGICPACKET);
        bytesToSend.push(id);
        var array = new Uint8Array(bytesToSend);
        client.write(new Uint8Array(bytesToSend));
        client.on('data', function (data)
        {
            console.log(`>> data received : ${data} `);
            client.end();   
            client.destroy();
            delete client;
            callback(data.toString());  // return true for successful
        });
    });
    client.on('error', err => 
    {
        console.log(`Zone action error. ZoneId: ${id}. Error: ${err}`);
        callback('Zone action error. Error: ' + err);

        client.end();
        client.destroy();
        delete client;
    });
}

function onClientConnection(sock)  //on arduino connection
{
    //Log when an arduino connnects.
    console.log(`\n\n\n ${sock.remoteAddress}:${sock.remotePort} Connected: ` + new Date().toISOString().replace(/T/, ' ').replace(/\..+/, ''));

    sock.on('data', function (data)
    {
        console.log(`>> data received : ${data} `);
        //sock.end();

        var zonesUpdated = [];

        for (let i=0; i < data.length; i+=2)
        {
            console.log("Zone id: " + data[i]);
            console.log("Zone state: " + data[i+1]);

            var zoneIndex = exports.zones.findIndex(o => o.id == data[i]);
            if (exports.zones[zoneIndex].state != data[i+1])  //check to see if state actually changed. the keep alive updates send redundant info.
            {
                zonesUpdated.push(exports.zones[zoneIndex]);

                exports.zones[zoneIndex].timeStamp = new Date();
            }
            exports.zones[zoneIndex].state = data[i+1];
            
            exports.zones[zoneIndex].isAlive = true;

            exports.zones[zoneIndex].ipAddress = sock.remoteAddress;
        }
        if (zonesUpdated.length > 0)
        {
            androidDeviceManager.informDevices(zonesUpdated);
            //speakerManager.inform(zonesUpdated); 
            loggingManager.logZoneEvents(zonesUpdated);
        }

        sock.destroy();
        //sock.write("MessageRecieved");

    });

    sock.on('close', function ()
    {
        console.log(`${sock.remoteAddress}:${sock.remotePort} Connection closed`);
    });

    sock.on('error', function (error)
    {
        console.error(`${sock.remoteAddress}:${sock.remotePort} Connection Error: ${error}`);
    });
};
