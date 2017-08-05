<?php
header('Content-Type: text/event-stream');
header('Cache-Control: no-cache');

$fp = popen('/home/pi/git/RGPIO/run/runTCPClient  server=localhost port=2603', 'r');
 while(!feof($fp))
    {
        $s=fread($fp, 512);
        echo "data: ".$s."\n\n";
        ob_flush();
        flush();
    }
pclose($fp);


?>

