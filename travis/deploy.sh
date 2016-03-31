#!/bin/sh

set -e

mkdir -p git/snapshots/com/agifac/lib/app-framework/$1

cp ~/.m2/repository/com/agifac/lib/app-framework/$1/*.jar git/snapshots/com/agifac/lib/app-framework
cp ~/.m2/repository/com/agifac/lib/app-framework/$1/*.zip git/snapshots/com/agifac/lib/app-framework
cp ~/.m2/repository/com/agifac/lib/app-framework/$1/*.pom git/snapshots/com/agifac/lib/app-framework

cd git
git init

git config user.name "Travis CI"
git config user.email "marc.schar@the-agile-factory.com"

git add .
git commit -m "Deploy to GitHub Pages"

git push --force --quiet "https://${GH_TOKEN}@${GH_REF}" master:gh-pages > /dev/null 2>&1
