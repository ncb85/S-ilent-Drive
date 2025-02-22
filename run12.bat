@ECHO OFF
CALL set_java_home.bat
SET RUNTIME=%JAVA_HOME%\bin\java
SET ARGS=-Dspring.config.location=application120.yml
SET ARGS=%ARGS% -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
SET ARGS=%ARGS% -jar
SET TARGET=target\silent-drive-1.0.2-SNAPSHOT.jar
%RUNTIME% %ARGS% %TARGET%
