const { time } = require('console');
var net = require('net');
const server = net.createServer(onClientConnection);
const sqlManager = require('./sqlManager.js');
const androidDeviceManager = require('./androidDeviceManager.js');
//var _recievedEventCallback;

exports.zones = [];


class Zone
{
    constructor(id, name, muted, state)
    {
        this.id = id;
        this.name = name;
        this.muted = muted;
        this.state = state;
        this.isAlive = false;
    }
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
        exports.zones.push(new Zone(entry.id, entry.name, entry.muted, -1));
    });
    //exports.userDevices = data.recordSet
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

function onClientConnection(sock)
{
    //Log when a client connnects.
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
            }
            exports.zones[zoneIndex].state = data[i+1];
            
            exports.zones[zoneIndex].isAlive = true;
        }
        if (zonesUpdated.length > 0)
            androidDeviceManager.informDevices(zonesUpdated);

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
