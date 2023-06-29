#!/bin/bash

# Updates all relevant files of $WVCCI_INST to github 'wv-cci-toolbox' repository.
# See https://github.com/bcdev/wv-cci-toolbox
# OD, 20220921

olddir=$(pwd)

cd workspace/git/wv-cci-toolbox
git pull -a
cp -p $WVCCI_INST/*.py src/main/bin/cems_shared/wvcci-inst
cp -p $WVCCI_INST/my* src/main/bin/cems_shared/wvcci-inst
cp -p $WVCCI_INST/*.bash src/main/bin/cems_shared/wvcci-inst
cp -p $WVCCI_INST/*.txt src/main/bin/cems_shared/wvcci-inst
cp -p $WVCCI_INST/src/*.py src/main/bin/cems_shared/wvcci-inst/src
cp -p $WVCCI_INST/bin/*sh src/main/bin/cems_shared/wvcci-inst/bin
git add src/main/bin/cems_shared/wvcci-inst
git commit -m "updated JASMIN WVCCI_INST scripts"
git push

cd $olddir

echo "done"
