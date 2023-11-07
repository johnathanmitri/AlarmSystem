# AlarmSystem

This is a work-in-progress Alarm System. The goal was to make a full Alarm System to use in my own house. 

The Frontend is an Android App written in Java that would be installed on our devices. 

The Backend is written in Node.js, with Microsoft SQL Server for a database. To monitor the zones (Doors/Windows), Arduinos are used. 

Monitoring a door is easy with a Magnet in the door, and a Magnetic Reed Switch in the door frame. To simulate this, I am simply opening and closing the circuit for each zone with a breadboard. 

Heres a demo of the zones being updated in real time as they are "Opened" and "Closed":

https://github.com/johnathanmitri/AlarmSystem/assets/28831749/faa8ea17-4ba5-41a7-85ee-6fcfc0a289cd

Users can view the event history of each zone:

https://github.com/johnathanmitri/AlarmSystem/assets/28831749/5ebdd234-56ec-415e-8cd7-803a16773907

If the app is closed, push notifications are sent to devices when a zone is opened:

https://github.com/johnathanmitri/AlarmSystem/assets/28831749/1b4b97fa-b3a9-4cef-b8b2-3562ea73513f


