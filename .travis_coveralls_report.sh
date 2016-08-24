#!/usr/bin/env bash

if [[ "${TRAVIS_JDK_VERSION}" != "oraclejdk8" ]]; then
    echo "Skipping Coveralls report for JDK version \"${TRAVIS_JDK_VERSION}\""
    exit
fi

mvn -B cobertura:cobertura coveralls:report