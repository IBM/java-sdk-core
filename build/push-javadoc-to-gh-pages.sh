#!/bin/bash

if [ "$TRAVIS_BRANCH" ]; then

  git config --global user.email "loganpatino10@gmail.com"
  git config --global user.name "lpatino10"
  git clone --quiet --branch=gh-pages https://${GITHUB_OAUTH_TOKEN}@github.com/IBM/java-sdk-core.git gh-pages > /dev/null
  echo "Cloned repo"

  pushd gh-pages
    # on tagged builds, $TRAVIS_BRANCH is the tag (e.g. v1.2.3), otherwise it's the branch name (e.g. master)
    rm -rf docs/$TRAVIS_BRANCH
    mkdir -p docs/$TRAVIS_BRANCH
    cp -rf ../target/apidocs/* docs/$TRAVIS_BRANCH
    ../.utility/generate_index_html.sh > index.html
    echo "did the other stuff"

  popd

  echo -e "Published Javadoc for build $TRAVIS_BUILD_NUMBER ($TRAVIS_JOB_NUMBER) on branch $TRAVIS_BRANCH.\n"

else

  echo -e "Not publishing docs for build $TRAVIS_BUILD_NUMBER ($TRAVIS_JOB_NUMBER) on branch $TRAVIS_BRANCH of repo $TRAVIS_REPO_SLUG"

fi
