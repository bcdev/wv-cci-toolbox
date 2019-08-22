#!/bin/bash
lftp <<SCRIPT
set ftps:initial-prot ""
set ftp:ssl-force true
set ftp:ssl-protect-data true
set ssl:verify-certificate no
open ftps://ftp.telespazio.com:990
user ODanne D3bc75ksd34fg6
#lcd /tmp
cd /WV_cci/Data/TCWV/Dataset1/dailies
mput WV_CCI_L3_tcwv_*.tar.gz
exit
SCRIPT
