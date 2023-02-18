var spawn = require('child_process').spawn;

const soundPlayer = spawn('./speakerManager/AlarmSystemSoundPlayer.exe');
soundPlayer.stdin.setEncoding('utf-8');

soundPlayer.stdout.on("data", (data)=>{
  console.log("" + data);
})

soundPlayer.stderr.on("data", (data)=>{
  console.log("" + data);
})

exports.inform = function(zonesUpdated)
{
  zonesUpdated.forEach(zone => 
  {
    if (zone.state != 1) //if it is not closed
    {
      var fileName = zone.name.replace(/\s+/g, '');  //remove spaces from the string
      soundPlayer.stdin.write(fileName + ".wav\n");
    }

  });

}

/*
function ask()
{
rl.question("Run command?\n", function (string) {
  userInput = string;

  process.stdout.write("You typed: " + userInput);
  soundPlayer.stdin.write("" + userInput + "\n");
  // close input stream

  ask();
});
}

ask();*/