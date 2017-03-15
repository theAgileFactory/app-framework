#!/bin/bash
if [ ! -z "$TRAVIS_TAG" ]
then
    echo "TAG : releasing $TRAVIS_TAG to Maven Central"
else
    echo "SNAPSHOT : releasing to Sonatype snapshot repository"
fi
mvn clean deploy  -Dgpg.passphrase="${GPG_PASSPHRASE}" --settings ~/.m2/settings.xml -DskipTests=true -B -U
