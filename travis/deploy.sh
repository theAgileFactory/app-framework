#!/bin/sh

set -e

mkdir -p git/snapshots/com/agifac/lib/app-framework/$1

cp ~/.m2/repository/com/agifac/lib/app-framework/$1/*.jar git/snapshots/com/agifac/lib/app-framework/$1
cp ~/.m2/repository/com/agifac/lib/app-framework/$1/*.pom git/snapshots/com/agifac/lib/app-framework/$1

#wget https://github.com/github/git-lfs/releases/download/v1.1.2/git-lfs-linux-amd64-1.1.2.tar.gz
#tar -xzf git-lfs-linux-amd64-1.1.2.tar.gz

cd git
git init

git config user.name "Travis CI"
git config user.email "marc.schar@the-agile-factory.com"

#../git-lfs-1.2.2/git-lfs install
#../git-lfs-1.2.2/git-lfs track *.zip
#../git-lfs-1.2.2/git-lfs track *.jar

git add snapshots
git commit -m "Deploy to GitHub Pages"

git push --force "https://${GH_TOKEN}@${GH_REF}" master:gh-pages
