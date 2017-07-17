<?php
header('Content-Type: text/event-stream');
header('Cache-Control: no-cache');

$fp = popen(' java -jar /home/pi/html/TCPClient.jar server=localhost port=2601', 'r');
 while(!feof($fp))
    {
        $s=fread($fp, 512);
        echo "data: ".$s."\n\n";
        ob_flush();
        flush();
    }
pclose($fp);


?>

