@echo off

@echo Started: %date% %time%
python meris_l2_processor_for_calvalus.py ^
4th_rp/ENV_ME_1_RRG____20100818T091436_20100818T095829_________________2633_092_122______DSI_R_NT____.SEN3_idepix2.nc ^
4th_rp/ENV_ME_1_RRG____20100818T091436_20100818T095829_________________2633_092_122______DSI_R_NT____.SEN3_idepix2_wv.nc
@echo Completed: %date% %time%