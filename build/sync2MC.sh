#!/bin/bash

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
