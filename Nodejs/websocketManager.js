const wsLibrary = require('ws');
const https = require('https');
const fs = require('fs');
//const sqlManager = require('./sqlManager.js');
const androidDeviceManager = require('./androidDeviceManager.js');
const arduinoManager = require("./arduinoManager");
const { Console } = require('console');

var wsPort = 3820;
var wssPort = 3819;

wsClients = [];

var idCount = 0;
function nextClientID()
{
    idCount++;
    return idCount; //code can overload, but Number.MAX_SAFE_INTEGER = 9007199254740991.
}

let rawdata = fs.readFileSync('./protected/password.json');
let auth = JSON.parse(rawdata);

const authPassTLS = auth.authPassTLS;
const authPassNoTLS = auth.authPassNoTLS;

const timer = setInterval(function ping()
{
    wsClients.forEach(function each(wsClient)
    {
        if (wsClient.destroyed)
        {
            console.log("Destroyed client in list, id" + wsClient.clientID);   //this is bad. this should never happen.
            return;
        }
        if (wsClient.authenticated)
        {
            if (wsClient.isAlive === false)
            {
                console.log(`Client timed out.`);
                wsClient.destroy();
                return;  //this is return because java foreach is a callback function.
            }

            wsClient.isAlive = false;
            wsClient.wsSocket.ping();
        }
        else //it is not authenticated. wait for authentication, or TIMEOUT.
        {
            if (wsClient.isAlive === false)
            {
                console.log(`Client authentication timed out.`);
                wsClient.send("AUTH TIMEOUT");
                wsClient.destroy();
                return;  //this is return because java foreach is a callback function.
            }

            wsClient.isAlive = false;
        }
    });
}, 5000);

class WsClient  //this represents each client that is connected over websocket, whether it is a valid device, authenticated, or not. It keeps track and verifies them.
{
    constructor(wsSocket, isTLS)
    {
        //this.name = "WSCLIENT!!";
        this.wsSocket = wsSocket;
        console.log(`new client connected:  ${this.wsSocket._socket.remoteAddress}:${this.wsSocket._socket.remotePort}`);

        this.isTLS = isTLS;
        this.isAlive = true;
        this.wsSocket.on("message", data => this.onMessage(data));
        // handling what to do when clients disconnects from server
        this.wsSocket.on("close", (closeEvent) =>
        {
            if (!this.destroyed)
            {
                console.log(`Client has disconnected`);
                this.destroy();
            }
        });
        this.wsSocket.on("error", () =>
        {
            console.log(`${this.wsSocket._socket.remoteAddress}:${this.wsSocket._socket.remotePort} has had an Error`)
            if (!this.destroyed)
            {
                this.destroy();
            }
        })

        this.wsSocket.on('pong', () => { this.isAlive = true; });

        this.authenticated = false;
        this.initialized = false;
        this.androidId = -1;
        this.destroyed = false;
        this.clientID = nextClientID();
        wsClients.push(this);//-1;
    }
    onMessage(data)
    {


        if (this.authenticated === false)
        { //we still have to authenticate and initialize this client
            try
            {
                var authAttempt = JSON.parse(data);
                var reqPass1, reqPass2;
                if (this.isTLS)
                {
                    reqPass1 = authPassTLS[0];
                    reqPass2 = authPassTLS[1];
                }
                else // is not TLS
                {
                    reqPass1 = authPassNoTLS[0];
                    reqPass2 = authPassNoTLS[1];
                }
                if (authAttempt.pass1 === reqPass1 && authAttempt.pass2 === reqPass2)
                {
                    //this.wsSocket.send("AUTH SUCCESS");
                    this.send(JSON.stringify({
                        intent: "auth",
                        status: "success"
                    }));

                    this.authenticated = true;
                    this.isAlive = true;
                }
                else
                { // AUTH FAIL
                    //this.wsSocket.send("AUTH FAIL");
                    this.send(JSON.stringify({
                        intent: "auth",
                        status: "failure"
                    }));
                    this.destroy();
                }
            }
            catch
            { //in this case, client sent invalid JSON data, so auth fail   
                //this.wsSocket.send("AUTH FAIL");
                this.send(JSON.stringify({
                    intent: "auth",
                    status: "failure"
                }));
                this.destroy();
            }
        }
        else if (this.initialized === false)  //we have authenticated, but we still need to initialize.
        {
            try
            {
                var jsonData = JSON.parse(data);

                if (jsonData.intent == "register")
                {
                    if (typeof jsonData.fcmToken === 'undefined' || typeof jsonData.deviceName === 'undefined' || typeof jsonData.notifsEnabled === 'undefined')
                        throw new Error();

                    androidDeviceManager.registerNewDevice(this, jsonData.deviceName, jsonData.fcmToken, jsonData.notifsEnabled, newId =>
                    {
                        this.androidId = newId;
                        this.send(JSON.stringify({
                            intent: "register",
                            status: "success",
                            androidId: newId
                        }));
                        this.initialized = true;
                        this.send(JSON.stringify(
                            {
                                intent: "updateZones",
                                zones: arduinoManager.zones
                            }));
                    });
                }
                else if (jsonData.intent == "connect")
                {
                    if (typeof jsonData.androidId === 'undefined')
                        throw new Error();

                    androidDeviceManager.connectDevice(this, jsonData.androidId, status=>
                    {  //this is the success callback. failure is managed in the androidDeviceManager class.
                        this.send(JSON.stringify({
                            intent: "connect",
                            status: status,
                            androidId: jsonData.androidId
                        }));
                        if (status==="success")
                        {
                            this.androidId = jsonData.androidId;
                            this.initialized = true;

                            this.send(JSON.stringify(
                                {
                                    intent: "updateZones",
                                    zones: arduinoManager.zones
                                }));
                        }
                        else
                        {
                            this.destroy();
                        }
                        
                    });
                    
                }
                else
                    throw new Error();
            }
            catch (e)
            {
                this.send(JSON.stringify({
                    intent: "initialize",
                    status: "Invalid JSON Data"
                }));
            }
        }
        else //we have recieved data from an initialized client. we can process requests now
        {
            console.log(`(Android) ${this.wsSocket._socket.remoteAddress}:${this.wsSocket._socket.remotePort} has sent us: ${data}`);
            //console.log("Data Recieved: " + data);
            var jsonData;
            try
            {
                jsonData = JSON.parse(data);
            }
            catch (e)
            {
                this.send(JSON.stringify({
                    intent: "response",
                    status: "Invalid JSON Data"
                }));
                return; 
            }
            try
            {
                androidDeviceManager.onMessageRecieved(this, jsonData);
            }
            catch (e)
            {
                this.send(JSON.stringify({
                    intent: "response",
                    status: "MESSAGE HANDLING ERROR"
                }));
                console.log("MESSAGE HANDLING ERROR");
                return; 
            }
        }
    }
    send(data)
    {
        this.wsSocket.send(data);
    }
    destroy()
    {
        this.destroyed = true;
        console.log(`Destroying: ${this.wsSocket._socket.remoteAddress}:${this.wsSocket._socket.remotePort}`);

        this.wsSocket.terminate();  //destroys the socket
        delete this.wsSocket;

        if (this.initialized)
            androidDeviceManager.disconnectDeviceById(this.androidId); //set this device as disconnected in the 'registered devices array'

        var indexToRemove = wsClients.findIndex(o => o.clientID === this.clientID);
        wsClients.splice(indexToRemove, 1); //removes this object from the websocketClient array 

        //delete this;  //destroys the wsClient object
    }
}

exports.startListening = function()
{
const wsServer = new wsLibrary.Server({ port: wsPort }, function ()
{
    console.log(`WebSocket server is running on port ${wsPort}`);
})

const httpsServer = https.createServer({
    cert: fs.readFileSync('./protected/chain.pem'),
    key: fs.readFileSync('./protected/key.pem')
});
const wssServer = new wsLibrary.WebSocketServer({ server: httpsServer });

wsServer.on("connection", ws => new WsClient(ws, false)); //gotta host two because router is shitty. connecting to domain name (public ip) within network only allows one device at a time

wssServer.on("connection", ws => new WsClient(ws, true));

httpsServer.listen(wssPort, () => { console.log(`WebSocket over TLS server is running on port ${wssPort}`) }); //,function(){console.log(`WebSocket server is running on port ${wsPort}`);}
}

