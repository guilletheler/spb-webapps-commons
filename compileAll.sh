#!/bin/bash

cd webapps-commons
mvn clean install
cd ..

cd pf-commons
mvn clean install
cd ..
