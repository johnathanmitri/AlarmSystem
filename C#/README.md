NodeJS has no real support for playing audio to the speaker. There are some libraries but they are all hacky, and don't work well.

This is a simple C# Audio player to play Alarm Notifications to the speakers. It first plays a ding sound (DingSound.wav), and then speaks the name of the zone that was opened. It is told which zone through stdin. 

Copy the release .exe into the nodejs directory, in the same folder as speakerManager.js
