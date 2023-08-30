sbatch -p short-serial \
--time-min=02:00:00 \
--time=04:00:00 \
--mem=16000 \
--chdir=/gws/nopw/j04/esacci_wv/odanne/wvcci-inst \
-o /gws/nopw/j04/esacci_wv/odanne/wvcci-inst/log/wvcci-l2-tcwv-modis-2016-05-01-0130.out \
-e /gws/nopw/j04/esacci_wv/odanne/wvcci-inst/log/wvcci-l2-tcwv-modis-2016-05-01-0130.err \
--open-mode=append \
--job-name=wvcci-l2-tcwv-modis-2016-05-01-0130 \
--wrap="/gws/nopw/j04/esacci_wv/odanne/wvcci-inst/./bin/wvcci-l2-tcwv-modis-bash-slurm.sh \
/gws/nopw/j04/esacci_wv/odanne/WvcciRoot/L1b/MODIS_AQUA/2016/05/01/MYD021KM.A2016122.0130.061.2018057220534.hdf \
MYD021KM.A2016122.0130.061.2018057220534.hdf \
/gws/nopw/j04/esacci_wv/odanne/WvcciRoot/ModisCloudMask/MYD35_L2/2016/05/01/MYD35_L2.A2016122.0130.061.2018057220705.hdf \
MODIS_AQUA 2016 05 01 /gws/nopw/j04/esacci_wv/odanne/WvcciRoot "
