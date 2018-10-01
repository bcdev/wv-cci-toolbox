#!/bin/bash


###################################################################
#this program is about creating brwose images frm albedo netcdf files
#author: Said Kharbouche, MSSL.UCL(2014)
####################################################################




###################################################### INPUTs #####################################################
###################################################################################################################
#list of netcdf albedo files
LIST=$1
#output directory
OUTDIR=$2
#scripts and colorlut directory
HOME=/group_workspaces/cems/globalalbedo/scripts/
###################################################################################################################






#create output directory
mkdir -p $OUTDIR


idxDate=4
SIZE05='720x360'
SIZE005='7200x3600'

COLORTXT='white'

PYTHON1=$HOME/python/ncalbedo2png.py
BANDS1=BHR_NIR,BHR_VIS,BHR_SW,DHR_NIR,DHR_VIS,DHR_SW
MINMAX1=0:1,0:1,0:1,0:1,0:1,0:1
LUT1=$HOME/params/color_lut.txt
BANDSname1=$BANDS1


PYTHON2=$HOME/python/ncalbedo2png.py
BANDS2=Relative_Entropy,Weighted_Number_of_Samples
MINMAX2=1:11,0:30
BANDSname2=RelEntropy,WNSamples
LUT2='none'

PYTHON3=$HOME/python/ncalbedo2CoVpng.py
BANDS3=BHR_sigmaSW,BHR_SW
BANDSname3=BHR_SW_CoV
MINMAX3=0:1,0:1
LUT3='none'

PYTHON4=$HOME/python/ncalbedo2RGBpng.py
BANDS4=BHR_SW,BHR_NIR,BHR_VIS
BANDSname4=$BANDS4
MINMAX4=0:1,0:1,0:1
LUT4=''

while read line
do

	bn=$(basename $line)

	SIZE='none'

	if [[ "$bn" =~ '.005.' ]]
	then
		SIZE=$SIZE005
	fi

	if [[ "$bn" =~ '.05.' ]]
	then
        	SIZE=$SIZE05
	fi

        echo -e "\n\n\n-------------------------------------------------------------"
	echo python2.7 ${PYTHON1} $line $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1
        python2.7 ${PYTHON1} $line $OUTDIR  $BANDS1  $MINMAX1  $LUT1  $SIZE $idxDate $COLORTXT $BANDSname1
	echo -e "\n\n\n-------------------------------------------------------------"
        echo python2.7 ${PYTHON2} $line $OUTDIR  $BANDS2  $MINMAX2  $LUT2  $SIZE $idxDate $COLORTXT $BANDSname2
	python2.7 ${PYTHON2} $line $OUTDIR  $BANDS2  $MINMAX2  $LUT2  $SIZE $idxDate $COLORTXT $BANDSname2
        echo -e "\n\n\n-------------------------------------------------------------"
        echo python2.7 ${PYTHON3} $line $OUTDIR  $BANDS3  $MINMAX3  $LUT3  $SIZE $idxDate $COLORTXT $BANDSname3
        python2.7 ${PYTHON3} $line $OUTDIR  $BANDS3  $MINMAX3  $LUT3  $SIZE $idxDate $COLORTXT $BANDSname3
	echo -e "\n\n\n-------------------------------------------------------------"
        echo python2.7 ${PYTHON4} $line $OUTDIR  $BANDS4  $MINMAX4  $LUT4  $SIZE $idxDate $COLORTXT $BANDSname4
        python2.7 ${PYTHON4} $line $OUTDIR  $BANDS4  $MINMAX4  $LUT4  $SIZE $idxDate $COLORTXT $BANDSname4


done<$LIST

echo -e "\n\n\nDone."





