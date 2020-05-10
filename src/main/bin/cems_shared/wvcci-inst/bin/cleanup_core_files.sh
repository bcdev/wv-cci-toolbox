#!/bin/bash

# cleanup core files and move hs_err files every 5min...
while true
do
  echo "`date` : cleanup core files..."

  echo "rm -f $WVCCI_INST/core.*"
  rm -f $WVCCI_INST/core.*

  echo "rm $WVCCI_INST/hs_err*"
  rm -f $WVCCI_INST/hs_err*

  sleep 300
done

