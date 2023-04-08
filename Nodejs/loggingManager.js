const fs = require("fs");

const zonePath = "logs/zones/";

var stream = fs.createWriteStream("append.txt", {flags:'a'});

function getFileName(zoneId)
{
    return zonePath + zoneId + ".bin";
}

exports.logZoneEventsJson = function(zonesUpdated)
{
    zonesUpdated.forEach(function(zone)
    {
        stream.write(JSON.stringify({
            id: zone.id, 
            name: zone.name,
            state: zone.state, 
            timeStamp: zone.timeStamp
        }) + "\n"); //push this zone event as a string
    });

}

exports.logZoneEvents = function(zonesUpdated)
{
    zonesUpdated.forEach(function(zone)
    {
        //             0x3F is 00111111, and it is shifted left two bits. this makes the upper 6 bits
        //             0x03 is 00000011. this will be the lower two bits.l
        //             subtract 1 from zone id to make it 0 based index, since sql starts at 1
        //             add 1 to state to make it 0 to 2 instead of -1 to 1
        //var combined = (((zone.id-1) & 0x3F) << 2) | ((zone.state + 1) & 0x03); 
        const buffer = Buffer.alloc(9); //9 byte buffer
        buffer.writeInt8(zone.state, 0); // write the combined byte at offset 0.
        buffer.writeBigInt64LE(BigInt(zone.timeStamp.getTime()), 1);   // write the milliseconds since January 1, 1970, at offset 1. This is a 64 bit signed integer.
        fs.appendFile(getFileName(zone.id), buffer,  "binary", function(err) { });

        /*stream.write(JSON.stringify({
            id: zone.id, 
            name: zone.name,
            state: zone.state, 
            timeStamp: zone.timeStamp
        }) + "\n"); //push this zone event as a string*/
    });

}

/*
exports.logZoneEvents = function(zonesUpdated)
{
    zonesUpdated.forEach(function(zone)
    {
        zoneEventStringList.push(JSON.stringify({
            id: zone.id, 
            name: zone.name,
            state: zone.state, 
            timeStamp: zone.timeStamp
        })); //push this zone event as a string
    });

    if (zoneEventStringList.length >= 10)
    {
        list = zoneEventStringList;
        zoneEventStringList = [];
        fs.appendFile('zoneEventLog.txt', list.join("\n") + "\n", function (err) {
            if (err) throw err;
            console.log('Saved logs to disk');
          });
    }
}*/

exports.getZoneEvents = function(zoneId, callback)
{
    const EVENTS_COUNT = 50;
    var bytesToRead = EVENTS_COUNT * 9;
 
    if (!fs.existsSync(getFileName(zoneId)))
    {
        callback(Buffer.alloc(0));
        return;
    }
    const fileSize = fs.statSync(getFileName(zoneId)).size;
    
    if (fileSize < bytesToRead)
        bytesToRead = fileSize;
    
    var position = fileSize - bytesToRead;

    const buffer = Buffer.alloc(bytesToRead); 

    fs.open(getFileName(zoneId), 'r+', function (err, fd) 
    {
        if (err) {
            return console.error(err);
        }
        fs.read(fd, buffer, 0, bytesToRead, position, function(err, bytesRead, buffer){
            if (err)
                callback(Buffer.alloc(0));
            else
                callback(buffer);
            fs.close(fd);
        });
    });

    
    //return buffer;
    /*
    getFileName(zone.id)

    var arr = [];

    var lineReader = require('readline').createInterface({
        input: fs.createReadStream('append.txt')
      });
      
      lineReader.on('line', function (line) 
      {
        var obj = JSON.parse(line);
        if (obj.id == zoneId)
            arr.push(obj);
      });
      lineReader.on('close', function()
      {
        callback(arr);
      });*/
      
}

exports.getZoneEventsJson = function(zoneId, callback)
{
    var arr = [];

    var lineReader = require('readline').createInterface({
        input: fs.createReadStream('append.txt')
      });
      
      lineReader.on('line', function (line) 
      {
        var obj = JSON.parse(line);
        if (obj.id == zoneId)
            arr.push(obj);
      });
      lineReader.on('close', function()
      {
        callback(arr);
      });
      
}