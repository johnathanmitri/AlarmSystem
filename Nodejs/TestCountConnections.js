var arduinoManager = require("./arduinoManager.js");

//var notificationManager = require('./notificationManager.js');

var count = 0;
function testFunc(data)
{
  count++;
  console.log(data.toString('ascii') + ", Count: " + count);
}

arduinoManager.startListening(testFunc);

//arduinoManager.startListening(notificationManager.notifySingleDevice);