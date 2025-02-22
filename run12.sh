# LINUX versin of run script
#
ARGS="-Dspring.config.location=application120.yml"
#ARGS="${ARGS} -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
ARGS="${ARGS} -jar"
TARGET="target/silent-drive-1.0.2-SNAPSHOT.jar"
java $ARGS $TARGET
