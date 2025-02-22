@ECHO OFF
REM using JAVA JDK Version15
REM SET JAVA_HOME="C:\Program Files\Java\jdk-17"
CALL SET_JAVA_HOME
SET RUNTIME=%JAVA_HOME%\bin\java
SET ARGS=-Dspring.config.location=application144.yml
SET ARGS=%ARGS% -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
SET ARGS=%ARGS% -jar
SET TARGET=target\silent-drive-1.0.2-SNAPSHOT.jar
%RUNTIME% %ARGS% %TARGET%
