#!/bin/bash

SCRIPTPATH=`dirname $0`
cd $SCRIPTPATH

java -cp ".:lib/*" dk.frv.aisrecorder.AisRecorder
