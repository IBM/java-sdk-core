#!/bin/bash
#
# sync2MC.sh - invoke the "maven_central_sync" bintray REST API to request that the specified 
#              bintray package be synced to maven central.
#
# Syntax:
#    sync2MC.sh <bintray-user> <bintray-apikey> <bintray-reponame> <bintray-packagename> <bintray-packageversion>
# where:
#   <bintray-user>           is the bintray username (subject)
#   <bintray-apikey>         is the api key associated with the bintray user
#   <bintray-reponame>       is the name of the bintray repository that the package belongs to
#   <bintray-packagename>    is the name of the bintray package to be synced
#   <bintray-packageversion> is the version of the package to be synced
#
# example:
#   sync2MC.sh bintrayuser1 <apikey> my-bintray-maven-repo my.group.id:my-package-name 1.1.0
#
#
if [ $# -lt 5 ]
then
    echo "
 Syntax:  
      $0 <bintray-user> <bintray-apikey> <bintray-reponame> <bintray-packagename> <bintray-packageversion>>
 Example:
      $0 user1 A1098765 my-bintray-repo1 my-bintray-package 0.0.1
"

 exit 1
fi

subject=$1
apikey=$2
reponame=$3
pkgname=$4
pkgversion=$5

#set -x

urlstring="https://api.bintray.com/maven_central_sync/${subject}/${reponame}/${pkgname}/versions/${pkgversion}"

basicauth="${subject}:${apikey}"

echo "
Executing curl command..."
curl -X POST --data '{ "close": "1" }' -H "Content-Type: application/json" -L -k --user ${basicauth} ${urlstring}
