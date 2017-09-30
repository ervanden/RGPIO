
g_icons = {
 "heating" : heatingIcon,
 "level" : levelIcon,
 "temp" : tempIcon,
 "1622821" : relayIcon
}


function heatingIcon(heating) {
// console.log("heatingIcon is called value="+heating.value);
 if (heating.value == "High") return "flame"; else return "cold";
}

function tempIcon(device){ return "thermometer";}


function levelIcon(device){

var l=Number(device.avg);
var lmax=200;
var p=l/lmax;
if (p<0) return "level10";
if (p<0.1) return "level9";
if (p<0.2) return "level8";
if (p<0.3) return "level7";
if (p<0.4) return "level6";
if (p<0.5) return "level5";
if (p<0.6) return "level4";
if (p<0.7) return "level3";
if (p<0.8) return "level2";
if (p<0.9) return "level1";
return "level0";
}

function relayIcon(relay) {
// console.log("relayIcon is called 0="+relay["0"]);
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


  distance=obj.00;   // distance is the value of the analog output 00.
  if (distance <10) return "reservoir100"; // reservoir almost full
  if (distance <50) return "reservoir75"; // reservoir 3/4 full
  if (distance <90) return "reservoir50"; // reservoir 1/2 full
  if (distance <130) return "reservoir25"; // reservoir 1/4 full
  return reservoir0"  // reservoir almost empty 
}
*/
