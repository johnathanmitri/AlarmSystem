const sql = require('mssql')
const fs = require('fs');


let rawdata = fs.readFileSync('./protected/sqlConfig.json');
let config = JSON.parse(rawdata);


//exports.initialize = function()

function makeQuery(sqlQuery, callback)
{
    sql.connect(config, function (err)
    {
        if (err)
        {
            console.log("SQL connection error: " + err);
            callback("error");
            return;
        }
        let sqlRequest = new sql.Request();

        sqlRequest.query(sqlQuery, function (err, data)
        {
            if (err)
            {
                console.log(err);
                callback("error");
                return;
            }
            callback(data);
            //userDevices = data.recordset;
        });
    });
}
exports.makeQuery = makeQuery;


exports.addDevice = function (deviceName, regToken, notifsEnabled, callback) //returns id
{
    sql.connect(config, function (err)
    {
        if (err)
        {
            console.log("SQL connection error: " + err);
            return;
        }
        let sqlRequest = new sql.Request();
        sqlRequest.input('deviceName', sql.Char, deviceName);
        sqlRequest.input('regToken', sql.Char, regToken);
        sqlRequest.input('notifsEnabled', sql.Bit, notifsEnabled);
        
        let sqlQuery = `INSERT INTO dbo.FcmDevices(name, regToken, notifsEnabled) OUTPUT INSERTED.ID VALUES (@deviceName, @regToken, @notifsEnabled);`;

        sqlRequest.query(sqlQuery, function (err, data)
        {
            if (err)
            {
                console.log(err);
                return;
            }

            //exports.registeredDevices.push(new regDevice(data.recordset[0].ID, deviceName, regToken, true)); //add the newly registered device to our local array
            callback(data.recordset[0].ID);

            //callback(new regDevice(data.recordset[0].ID, deviceName, regToken, true));   this was a short lived idea. not a good idea though.

            //console.log("Data returned from insert: " + data);
        });
    });
    //return id;
    
}
