#!/bin/bash

# Required environment variables:
# GH_TOKEN
# GH_REPO_SLUG
# GH_TAG

printf "\n>>>>> Publishing javadoc for release build: repo=%s tag=%s\n" ${GH_REPO_SLUG} ${GH_TAG}

printf "\n>>>>> Cloning repository's gh-pages branch into directory 'gh-pages'\n"
rm -fr ./gh-pages
git clone --branch=gh-pages https://${GH_TOKEN}@github.com/IBM/java-sdk-core.git gh-pages

printf "\n>>>>> Finished cloning...\n"

pushd gh-pages

# Create a new directory for this branch/tag and copy the javadocs there.
printf "\n>>>>> Copying javadocs to new directory: docs/%s\n" ${GH_TAG}
rm -rf docs/${GH_TAG}
mkdir -p docs/${GH_TAG}
cp -rf ../target/site/apidocs/* docs/${GH_TAG}

printf "\n>>>>> Generating gh-pages index.html...\n"
../build/generateJavadocIndex.sh > index.html

printf "\n>>>>> Committing new javadoc...\n"
git add -f .
git commit -m "docs: latest javadoc for ${GH_TAG}"
git push -f origin gh-pages

popd

printf "\n>>>>> Published javadoc for release build: repo=%s tag=%s\n"  ${GH_REPO_SLUG} ${GH_TAG}

