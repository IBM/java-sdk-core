#!/bin/bash

# Publish javadocs only for a tagged-release.
if [[ -n "${TRAVIS_TAG}" ]]; then

    printf "\n>>>>> Publishing javadoc for release build: repo=%s branch=%s build_num=%s job_num=%s\n" ${TRAVIS_REPO_SLUG} ${TRAVIS_BRANCH} ${TRAVIS_BUILD_NUMBER} ${TRAVIS_JOB_NUMBER} 

    printf "\n>>>>> Cloning repository's gh-pages branch into directory 'gh-pages'\n"
    rm -fr ./gh-pages
    git clone --branch=gh-pages https://${GH_TOKEN}@github.com/IBM/java-sdk-core.git gh-pages

    printf "\n>>>>> Finished cloning...\n"

    pushd gh-pages
    
    # Create a new directory for this branch/tag and copy the javadocs there.
    printf "\n>>>>> Copying javadocs to new directory: docs/%s\n" ${TRAVIS_BRANCH}
    rm -rf docs/${TRAVIS_BRANCH}
    mkdir -p docs/${TRAVIS_BRANCH}
    cp -rf ../target/site/apidocs/* docs/${TRAVIS_BRANCH}

    printf "\n>>>>> Generating gh-pages index.html...\n"
    ../build/generateJavadocIndex.sh > index.html

    printf "\n>>>>> Committing new javadoc...\n"
    git add -f .
    git commit -m "doc: latest javadoc for ${TRAVIS_BRANCH} (${TRAVIS_COMMIT})"
    git push -f origin gh-pages

    popd

    printf "\n>>>>> Published javadoc for release build: repo=%s branch=%s build_num=%s job_num=%s\n"  ${TRAVIS_REPO_SLUG} ${TRAVIS_BRANCH} ${TRAVIS_BUILD_NUMBER} ${TRAVIS_JOB_NUMBER} 

else

    printf "\n>>>>> Javadoc publishing bypassed for non-release build: repo=%s branch=%s build_num=%s job_num=%s\n" ${TRAVIS_REPO_SLUG} ${TRAVIS_BRANCH} ${TRAVIS_BUILD_NUMBER} ${TRAVIS_JOB_NUMBER} 

fi

