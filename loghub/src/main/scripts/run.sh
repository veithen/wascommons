#!/bin/sh
if [ -z $JAVA_HOME ]; then
  echo "You must set the JAVA_HOME variable before running this script."
  exit 1
fi

LOGHUB_HOME=`dirname $0`

CP=""
for jar in $LOGHUB_HOME/lib/*.jar; do
  CP=$CP:$jar
done

$JAVA_HOME/bin/java -Xms16m -Xmx32m -classpath $CP com.googlecode.wascommons.loghub.LogHub $LOGHUB_HOME/hub-log4j.xml
