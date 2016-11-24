#!/bin/bash

cd ..
sbt appmgr:packageBin
cp -R target/appmgr docker/ems
docker build -t trondheimdc/ems docker
rm -rf docker/ems
docker push trondheimdc/ems