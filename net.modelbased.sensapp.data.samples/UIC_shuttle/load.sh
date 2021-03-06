#!/bin/bash
# author: Sebastien Mosser

## Loading the SensApp Bash API
source ../api.sh

## Registering sensors
register_sensor "chicago/uic/shuttle/phi" \
                sensor_descriptors/chicago_uic_shuttle_phi.json
register_sensor "chicago/uic/shuttle/lambda" \
                sensor_descriptors/chicago_uic_shuttle_lambda.json
register_composite "chicago/uic/shuttle" \
                   sensor_descriptors/chicago_uic_shuttle.json

for file in `ls data/trip*.json`
do
    db_raw_import $file
done

