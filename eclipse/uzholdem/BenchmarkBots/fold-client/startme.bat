IF "%2"=="" GOTO ERROR
java -server -Xmx1G -Xms500M  -cp uzholdem.jar;meerkat-api.jar;pokerserver.jar;  uzholdem.bot.pokerserver.benchmarkbots.AlwaysFold %1 %2 

GOTO END
rem  -Xrunjdwp:transport=dt_socket,address=127.0.0.1:8765
:ERROR
@ECHO Usage: startme.bat <ipaddress> <portnumber>

:END