#!/bin/bash

SCRIPTPATH=`dirname $0`
cd $SCRIPTPATH

stop () {
	# Find pid
	PID=`./getpid.pl aisrecorder.AisRecorder`
	if [ -z $PID ]; then
		echo "AisRecorder not running"
		exit 1
	fi
	echo "Stopping AisRecorder"
	kill $PID
    exit 0
}


case "$1" in
start)
	PID=`./getpid.pl aisrecorder.AisRecorder`
	if [ ! -z $PID ]; then
		echo "AisRecorder already running"
		exit 1
	fi
    echo "Starting AisRecorder"
    ./aisrecorder.sh > /dev/null 2>&1 &
    ;;
stop)
    stop
    ;;
restart)
    $0 stop
    sleep 1
    $0 start
    ;;
*)
    echo "Usage: $0 (start|stop|restart|help)"
esac




