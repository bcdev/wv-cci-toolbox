#!/bin/bash
#$1 is the file name
#usage:this_script <filename>
HOST=incoming.dwd.de
USER=cmsaf
PASSWD=A!YuuV=QU3tULTkJybI3vdwo!dZ
FILE=$1
REMOTEPATH='water'

ftp -n $HOST <<END_SCRIPT
quote USER $USER
quote PASS $PASSWD
cd $REMOTEPATH
bin
put $FILE .$FILE 
rename .$FILE $FILE 
quit
END_SCRIPT
exit 0

