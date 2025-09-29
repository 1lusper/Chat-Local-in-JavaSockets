@echo off
setlocal

rem ---- Compila tudo que estiver na pasta ----
echo Compilando arquivos .java ...
javac *.java
if errorlevel 1 goto COMP_ERR

rem ---- Decide o que rodar pelo 1o argumento ----
if /I "%~1"=="server"      goto RUN_SERVER
if /I "%~1"=="client"      goto RUN_CLIENT
if /I "%~1"=="servergui"   goto RUN_SERVERGUI
if /I "%~1"=="clientgui"   goto RUN_CLIENTGUI

echo Uso: chat.bat server  /  client  /  servergui  /  clientgui
goto END

:RUN_SERVER
echo Iniciando servidor console...
java ChatServer
goto END

:RUN_CLIENT
echo Iniciando cliente console...
java ChatClient
goto END

:RUN_SERVERGUI
echo Iniciando servidor GUI...
java ChatServerGUI
goto END

:RUN_CLIENTGUI
echo Iniciando cliente GUI...
java ChatClientGUI
goto END

:COMP_ERR
echo ===========================================
echo ERRO DE COMPILACAO.
echo Rode os comandos abaixo para ver o erro exato:
echo    javac ChatServerGUI.java
echo    javac ChatClientGUI.java
echo    javac ChatServer.java
echo    javac ChatClient.java
echo ===========================================

:END
endlocal
