var notificationManager = require('./notificationManager.js');
var websocketManager = require('./websocketManager.js');
const sqlManager = require('./sqlManager.js');
var arduinoManager = require("./arduinoManager.js");


exports.registeredDevices = [];    //this represents all the registered android users we have. each entry is a device. 
//they are in this list regardless of whether or not they are currently connected.

exports.onMessageRecieved = function(sender, msgObj)
{
    if (msgObj.intent === "muteZone")
    {
        sqlManager.makeQuery(`UPDATE dbo.Zones SET muted = '${msgObj.value}' WHERE id = '${msgObj.id}';`, result => 
        {

        });
        zoneIndex = arduinoManager.zones.findIndex(o => o.id == msgObj.id);
        arduinoManager.zones[zoneIndex].muted = (msgObj.value == true); //dont know if this is necessary. just turning the string into a boolean.
        console.log(arduinoManager.zones[zoneIndex].name + " is now " + arduinoManager.zones[zoneIndex].muted);

        informZoneEdit(sender.androidId);
    }
}

class RegDevice //this represents a single registered device. 
{
    constructor(androidId, deviceName, fcmToken, notifsEnabled)
    {
        this.androidId = androidId;
        this.deviceName = deviceName;
        this.fcmToken = fcmToken;
        this.notifsEnabled = notifsEnabled;
        this.connected = false;             //is this entry in the list currently connected with a websocket
        this.wsClient;
    }
}

sqlManager.makeQuery("SELECT * FROM dbo.FcmDevices", result =>
{
    result.recordset.forEach(entry =>
    {
        exports.registeredDevices.push(new RegDevice(entry.id, entry.name, entry.regToken, entry.notifsEnabled));
    });
    //exports.userDevices = data.recordSet
});

exports.registerNewDevice = function (wsClient, deviceName, regToken, notifsEnabled, callback)
{
    sqlManager.addDevice(deviceName, regToken, notifsEnabled, newId =>
    {
        var newDevice = new RegDevice(newId, deviceName, regToken, notifsEnabled); //true represents "notifsEnabled". this is true by default.
        newDevice.connected = true;    //this device is already connected, since it is now being registered
        newDevice.wsClient = wsClient;

        exports.registeredDevices.push(newDevice);
        callback(newId);
    });

}

exports.connectDevice = function (wsClient, androidId, callback)
{
    var deviceIndex = exports.registeredDevices.findIndex(o => o.androidId == androidId);

    if (deviceIndex == -1)
    {
        console.log(`Connection Failure: Device id ${androidId} does not exist.`);
        callback(`Connection Failure: Device id ${androidId} does not exist.`);
        return;
    }
    else if (exports.registeredDevices[deviceIndex].connected == true)
    {
        console.log(`Connection Failure: Device Id ${androidId} is already connected.`);
        callback(`Connection Failure: Device Id ${androidId} is already connected.`);
        return;
    }

    exports.registeredDevices[deviceIndex].connected = true;
    exports.registeredDevices[deviceIndex].wsClient = wsClient;

    console.log("client connected id: " + androidId);
    callback("success");
}


exports.disconnectDeviceById = function (androidId)
{
    var indexToDisconnect = exports.registeredDevices.findIndex(o => o.androidId === androidId);
    if (indexToDisconnect != -1)
    {
        exports.registeredDevices[indexToDisconnect].connected = false;
        delete exports.registeredDevices[indexToDisconnect].wsClient;
    }
}
/*
exports.updateZone = function(data)
{
    //console.log("Zone id: " + data[0]);
    //console.log("Zone state: " + data[1]);


}*/

function informZoneEdit(deviceId)//deviceId represents the id of the device that initiated the update
{
    var zoneJsonListMessage = JSON.stringify(
    {
        intent: "updateZones",
        zones: arduinoManager.zones
    });
    exports.registeredDevices.forEach(function (regDevice)
    {
        if (regDevice.connected && regDevice.androidId != deviceId) 
        {
            regDevice.wsClient.send(zoneJsonListMessage);
        }
    });
}

exports.informDevices = function (zonesUpdated) //zonesUpdated is a list of objects that changed
{                                                  
    //var zone = arduinoManager.zones[zoneIndex];
    var fcmTokens = [];
//create zone json list
    var zoneJsonListMessage = JSON.stringify(
    {
        intent: "updateZones",
        zoneIdUpdated: -1,   //represents the ID assigned in sql server, not the index in the array.  NOT IMPLEMENTED!
        zones: arduinoManager.zones
    });

    
    exports.registeredDevices.forEach(function (regDevice)
    {
        if (regDevice.connected) //keep it as !(a == b) because deviceId is undefined most of the time. this way it handles it properly.
        {
            regDevice.wsClient.send(zoneJsonListMessage);
        }
        else //regDevice.connected is false
        {
            if (regDevice.notifsEnabled)
                fcmTokens.push(regDevice.fcmToken);
        }
    }); 
    zonesUpdated.forEach(function(zone){    //for each zone that changed
        if ((zone.state == 0 || zone.state == -1) && !zone.muted)    //if this zone requires a notification
            notificationManager.notifyDevices(fcmTokens, zone);
    });
    
    return; //FIX THE NOTIF MANAGER
}
