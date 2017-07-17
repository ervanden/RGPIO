# RGPIO
Remote GPIO controller for raspberry (java library)

When building things with Raspberry PI we discovered that it is very handy to use remote ESP devices besides 
the on-board GPIO pins; to extend the reach of the PI.

This library extends the PI4J library that controls the GPIO pins of a Raspberry PI (RASPI), 
to include the GPIO pins of remote devices. These remote devices can be ESPxxx or other RASPIs. 
The remote devices connect to the central RASPI via TCP/IP. 
The purpose is to easily control a number of distributed physical devices within reach of a WiFi network.
