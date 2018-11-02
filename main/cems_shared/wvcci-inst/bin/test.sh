#!/bin/bash

sensor=$1

if [ "$sensor" == "MODIS" ]
then
    sensor=${sensor}_TERRA
fi

echo "sensor: $sensor"

echo `date`
