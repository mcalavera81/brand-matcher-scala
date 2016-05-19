#!/usr/bin/env bash
(cd .. && sbt clean pack)
cp -r ../target/pack/ ./pack

#(cd mongo-seed && cp ../../src/main/resources/brands.json . && docker build -t mongo-seed . && rm brands.json)

docker build -t branding .
rm -r pack


