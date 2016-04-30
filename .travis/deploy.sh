#!/bin/bash
if [ ! -z "$TRAVIS_TAG" ]
then
    echo "TAG : releasing $TRAVIS_TAG to Maven Central"
else
    echo "SNAPSHOT : releasing to Sonatype snapshot repository"
fi
mvn -f pom2.xml deploy --settings ~/.m2/settings.xml -DskipTests=true -B -U
