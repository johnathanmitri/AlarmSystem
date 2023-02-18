const fs = require("fs");

var stream = fs.createWriteStream("append.txt", {flags:'a'});

exports.logZoneEvents = function(zonesUpdated)
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