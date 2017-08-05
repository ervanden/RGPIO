<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {

exec("/home/pi/git/RGPIO/run/runTCPClient  server=localhost port=2602 command=status", $lines);
echo implode("\n",$lines);
}
?>

