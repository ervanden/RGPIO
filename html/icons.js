
g_icons = {
 sensor : sensorIcon,
 "1622821" : heatingIcon
}

console.log(" in icons.js: "+g_icons["1622821"]);

function sensorIcon(sensor) {
 console.log("sensorIcon is called avg="+sensor.avg);
 if (sensor.avg > 15) return "fullglass"; else return "emptyglass";
}
function heatingIcon(relay) {
 console.log("heatingIcon is called 0="+relay["0"]);
 if (relay["0"]==="High") return "flame"; else return "cold";
}

/* The code in this file determines which icon is displayed for a device.

All icons are supposed to be in htmldir/RGPIO/icons.
The icons are loaded with the following name:

For a VIO,  the name of the VIO.
For a PDEV, the HWid of the PDEV, or else the model of the PDEV.

The icon selection can be custmized by adding a property to the object g_icons.
The name of the property is the name of the VIO or the HWid or model of the PDEV. 
The value of the property is the name of a function that should also be defined in this file.
The function has to return the base name of the icon that should be displayed.
The function is called with as argument an object that represents the device. 
The object has the properties that are displayed to the right of the icon.
For a PDEV, the physical pins are also properties of this object.

Example:

g_icons = {
 sensor : sensorIcon,
 lights : lightsIcon,
 distance : distanceIcon
}

function sensorIcon(device) {
 if (device.value > 100) return "redHand"; else return "greenHand";
}

function lightsIcon(device) {
 if (lights.value=="High") return "lightBulb"; else return "grayCircle";
}

function distanceIcon(obj){

  // this device measures the water level. 
  // There are icons showing a reservoir filled to different levels

  distance=obj.00;   // distance is the value of the analog output 00.
  if (distance <10) return "reservoir100"; // reservoir almost full
  if (distance <50) return "reservoir75"; // reservoir 3/4 full
  if (distance <90) return "reservoir50"; // reservoir 1/2 full
  if (distance <130) return "reservoir25"; // reservoir 1/4 full
  return reservoir0"  // reservoir almost empty 
}
o*/
