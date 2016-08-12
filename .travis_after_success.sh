#!/bin/bash

if [[ "${TRAVIS_JDK_VERSION}" != "oraclejdk8" ]]; then
    echo "Skipping after_success actions for JDK version \"${TRAVIS_JDK_VERSION}\""
    exit
fi

mvn -B cobertura:cobertura coveralls:report

if [[ -n ${TRAVIS_TAG} ]]; then
    echo "Skipping deployment for tag \"${TRAVIS_TAG}\""
    exit
fi

if [ "$TRAVIS_BRANCH" != "master" ]; then
    echo "Skipping deployment for branch \"${TRAVIS_BRANCH}\""
    exit
fi

if [[ -n ${TRAVIS_PULL_REQUEST} && "$TRAVIS_PULL_REQUEST" != "false" ]]; then
    echo "Skipping deployment for pull request \"${TRAVIS_PULL_REQUEST}\""
    exit
fi

bash $DEPLOY_DIR/publish.sh