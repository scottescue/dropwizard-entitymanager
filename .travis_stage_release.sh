#!/usr/bin/env bash

function abort {
    printf "Skipping deployment: ${1}\n" && exit 1
}

function is_valid_tag {
    if [[ ! $1 =~ ^v[0-9]\.[0-9]\.[0-9]\-[0-9]$ ]]; then
        return 1
    fi
}


# Source the POM properties file written during the build
source target/project.properties >/dev/null 2>&1

echo
echo "Deployment Environment:"
echo "  JDK Version: ${TRAVIS_JDK_VERSION}"
echo "  Travis Tag: ${TRAVIS_TAG}"
echo "  Travis Branch: ${TRAVIS_BRANCH}"
echo "  POM Version: ${POM_VERSION}"
echo "  POM URL: ${POM_URL}"
echo "  POM SCM Tag: ${POM_SCM_TAG}"
echo "  Dropwizard Version: ${DROPWIZARD_VERSION}"
echo


# Ensure a project version is specified in the POM
if [[ ! -n $POM_VERSION ]]; then
    abort "Artifact version is not specified in the POM"
fi

# Ensure a SCM tag is specified in the POM
if [[ ! -n $POM_SCM_TAG ]]; then
    abort "SCM tag is not specified in the POM"
fi

if [[ -n $TRAVIS_TAG ]]; then
    # This is a tag build

    # Ensure we're building a valid release tag
    if ! is_valid_tag $TRAVIS_TAG; then
        abort "Tag '${TRAVIS_TAG}' is not a properly formatted release tag"
    fi

    # Ensure the project has a valid release version configured in the POM
    if ! is_valid_tag "v${POM_VERSION}"; then
        abort "This is a tag build, but ${POM_VERSION} is not a valid release version"
    fi

    # Ensure the Git tag, project version, project URL, SCM tag, and Dropwizard version are all in agreement
    if [[ ! $POM_SCM_TAG == $TRAVIS_TAG ]]; then
        abort "SCM tag '${POM_SCM_TAG}' in the POM does not match the '${TRAVIS_TAG}' tag being built"
    fi
    if [[ ! $POM_SCM_TAG == "v$POM_VERSION" ]]; then
        abort "SCM tag '${POM_SCM_TAG}' in the POM does not match version '${POM_VERSION}' in the POM"
    fi
    if [[ ! "${POM_URL}" == "http://scottescue.com/dropwizard-entitymanager/${POM_VERSION}" ]]; then
        abort "Project URL (${POM_URL}) in the POM should be http://scottescue.com/dropwizard-entitymanager/${POM_VERSION}"
    fi
    if [[ ! $POM_VERSION == ${DROPWIZARD_VERSION}\-* ]]; then
        abort "Dropwizard version ${DROPWIZARD_VERSION} is not correct for project version ${POM_VERSION}"
    fi

    DEPLOY_TYPE="tagged release"
else

    # This is not a tag build, ensure the POM has a snapshot version
    if [[ ! $POM_VERSION == *-SNAPSHOT ]]; then
        # If the POM isn't versioned as a snapshot, just skip deployment; no need to fail the build
        printf "Skipping deployment: This is a snapshot build, but POM version '${POM_VERSION}' is not a snapshot\n"
        exit
    fi

    DEPLOY_TYPE="snapshot"
fi


# All validations have succeeded at this point. Let's publish!
printf "Deploying ${DEPLOY_TYPE} version ${POM_VERSION}\n\n"
bash $DEPLOY_DIR/publish.sh
