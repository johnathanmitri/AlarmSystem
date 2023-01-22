var arduinoManager = require("./arduinoManager.js");

var androidDeviceManager = require("./androidDeviceManager.js");

const readline = require("readline");

// create interface for input and output
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
});

// create empty user input
let userInput = "";

// question user to enter name
function ask()
{
rl.question("Run command?\n", function (string) {
  userInput = string;

  console.log("You typed: " + userInput);
    arduinoManager.zoneAction(3, function () {});
  // close input stream

  ask();
});
}

ask();

//arduinoManager.startListening(); now called after the sql query.
//arduinoManager.startListening(notificationManager.notifySingleDevice);
