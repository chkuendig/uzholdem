IF "%2"=="" GOTO ERROR
ECHO %PATH%
java -server -Xmx1G -Xms1G -Xdebug  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -ea -cp uzholdem.jar;meerkat-api.jar;pokerserver.jar; uzholdem.bot.pokerserver.UZHoldemClient %1 %2 

GOTO END
rem  
:ERROR
@ECHO Usage: startme.bat <ipaddress> <portnumber>

:END