#!/usr/bin/env bash
#docker-compose build
DATA_DIR=$1
BATCH_SIZE=$2

if [[ -z $DATA_DIR ]];
then
    echo "Missing Data Directory."
    exit 1
fi
echo "DATA:_DIR: $DATA_DIR"

if [ ! -f /$DATA_DIR/hotels.json ]; then
    echo "File ${DATA_DIR}/hotels.json not found!"
    exit 1
fi

if [[ -z $BATCH_SIZE ]];
then
    BATCH_SIZE=3500
fi

echo "Batch size set to $BATCH_SIZE"

export DATA_DIR
export BATCH_SIZE
docker-compose build
docker-compose up