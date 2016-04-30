#!/bin/bash
if [ ! -z "$TRAVIS_TAG" ]
then
    echo "on a tag -> set pom.xml <version> to $TRAVIS_TAG"
    mvn -f pom2.xml --settings ~/.m2/settings.xml versions:set -DnewVersion=$TRAVIS_TAG 1>/dev/null 2>/dev/null
else
    echo "not on a tag -> keep snapshot version in pom.xml"
fi
mvn -f pom2.xml clean deploy --settings ~/.m2/settings.xml -DskipTests=true -B -U
