var admin = require("firebase-admin");
var serviceAccount = require("./protected/firebaseAuth.json");

var userDevices;


/*const sql = require('mssql')

const config = {
    server: '',
    port: ,
    database: '',
    user: '',
    password: '',
    
    options: {  
        trustServerCertificate: true,
    }
};

sql.connect(config, function(err)
{
    if (err)
    {
        console.log("SQL connection error: " + err);
        return;
    }
    let sqlRequest = new sql.Request();

    let sqlQuery = "SELECT * FROM dbo.FcmDevices";

    sqlRequest.query(sqlQuery, function(err,data)
    {
        if (err)
        {
            console.log(err);
            return;
        }
        userDevices = data.recordset;
    });
});
*/

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

exports.notifyDevices = function (regTokens, zone)   //maybe use something like function(regTokens, zones, urgency) or something like that.
{
    if (!Array.isArray(regTokens) || regTokens.length == 0) //if its not an array or if the length is 0
    {
        console.log("no fcm devices to notify.");
        return;
    }
    var titleString;
    var bodyString;
    if (zone.state == 0) //if it is opened
    {
        titleString = " Opened";
        bodyString = " was opened at ";
    }
    else if (zone.state == -1) //if it is offline
    {
        titleString = " Is Offline";
        bodyString = " went offline at ";
    }

    console.log(regTokens);

    const message = {
        android: {
            priority: "high",  //make sure every high priority results in a notification.
            notification: {
                //image: 'https://image.cnbcfm.com/api/v1/image/106878527-1620223837055-106748412-1602881184740-biden.jpg?v=1620224062',
                title: zone.name + titleString,
                body: zone.name + bodyString + zone.timeStamp.toLocaleTimeString("en-US", {
                    timeZone: "America/Los_Angeles"
                  }),//data.toString('ascii'),
                channel_id: 'AlarmChannel',
                notification_priority: "PRIORITY_MAX",
                sound: 'default',
                //default_sound: true,
            },
        },
        tokens: regTokens,
    };

    admin.messaging().sendMulticast(message)
        .then((response) =>
        {
            if (response.failureCount > 0)
            {
                const failedTokens = [];
                response.responses.forEach((resp, idx) =>
                {
                    if (!resp.success)
                    {
                        failedTokens.push(regTokens[idx]);
                    }
                });
                console.log('List of tokens that caused failures: ' + failedTokens);
            }
        });
}


//exports.handleEvent = function(data)
{

}


/*
exports.notifySingleDevice = function (data)
{
    // This registration token comes from the client FCM SDKs.
    var regtoken = "";

    const message = {
        android: {
            notification:
            {
                sound: "default",
            },
            priority: "high",  //be careful with this. it is limited, and fcm may quietly reduce priority to normal if overused or something read online
            notification: {
                image: 'https://image.cnbcfm.com/api/v1/image/106878527-1620223837055-106748412-1602881184740-biden.jpg?v=1620224062',
                title: 'ALERT',
                body: data.toString('ascii'),
                channel_id: 'AlarmChannel',
                notification_priority: "PRIORITY_MAX",
                sound: 'default',
                default_sound: true,
            },
        },
        token: registrationToken,
    };

    // Send a message to the device corresponding to the provided
    // registration token.
    admin.messaging().send(message)
        .then((response) =>
        {
            // Response is a message ID string.
            console.log('Successfully sent message:', response);
        })
        .catch((error) =>
        {
            console.log('Error sending message:', error);
        });
}
*/