#!/bin/sh

# based on https://odoepner.wordpress.com/2012/02/17/shell-script-to-generate-simple-index-html/

echo '<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>IBM Java SDK Core</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" crossorigin="anonymous">
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>IBM Cloud Java SDK Core Library Documentation</h1>
    </div>
    <p>Javadoc by release:</p>
    <ul>'
echo ${TRAVIS_TAG} | sed 's/^.*/        <li><a href="docs\/&">Latest release<\/a><\/li>/'
ls docs | grep --invert-match index.html | sed 's/^.*/        <li><a href="docs\/&">&<\/a><\/li>/'
echo '    </ul>
</div>
</body>
</html>'
