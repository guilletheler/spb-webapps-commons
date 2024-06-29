#!/bin/bash

cd /home/prog/java_mvn/ppbridge/ppapi

GCEMPOS_JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

JAVA_HOME=$GCEMPOS_JAVA_HOME; mvn clean install

cd /home/prog/java_mvn/ppbridge/pprestclient

GCEMPOS_JAVA_HOME=/usr/java/default1_8/

JAVA_HOME=$GCEMPOS_JAVA_HOME; mvn clean install

cd /home/prog/java_mvn/ppbridge/ppbridge

GCEMPOS_JAVA_HOME=/usr/java/default1_8/

JAVA_HOME=$GCEMPOS_JAVA_HOME; mvn clean install

cd /home/prog/java_mvn/ppbridge
