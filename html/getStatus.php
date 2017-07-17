<?php
if ($_SERVER["REQUEST_METHOD"] == "POST") {

exec("java -jar /home/pi/RGPIO/RGPIO.jar tcpclient server=localhost port=2602 command=status", $lines);
echo implode("\n",$lines);
}
?>

