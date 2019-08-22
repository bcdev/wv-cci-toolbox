#!/bin/bash

for FILE in $(ls *.gz); do
  echo "ftp_single_file_to_dwd_with_rename.sh $FILE"
  ./ftp_single_file_to_dwd_with_rename.sh $FILE
done


