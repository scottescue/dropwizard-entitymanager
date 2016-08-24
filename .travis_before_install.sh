#!/usr/bin/env bash

// Exit with a failing status code if the current branch is not master or a release branch
if ! [[ $TRAVIS_BRANCH == "master" || $TRAVIS_BRANCH =~ ^release\/.*$ ]]; then
    echo "Skipping build for branch \"${TRAVIS_BRANCH}\""
    exit 1
fi