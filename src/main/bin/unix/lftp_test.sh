#!/bin/bash
lftp <<SCRIPT
set ftps:initial-prot ""
set ftp:ssl-force true
set ftp:ssl-protect-data true
set ssl:verify-certificate no
open ftps://ftp.telespazio.com:990
user ODanne D3bc75ksd34fg6
#lcd /tmp
cd /WV_cci/
mput foo*.txt
exit
SCRIPT
