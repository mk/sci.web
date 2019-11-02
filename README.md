# sci.web

An online playground for the [Small Clojure Interpreter](https://github.com/borkdude/sci)
hosted [here](https://borkdude.github.io/sci.web).

## Development

To get an interactive development environment run:

    clojure -A:fig:dev

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    rm -rf target/public out

## Build

Build a production version of sci.web:

    script/build

It will be written to the `dist` folder.

## Release

Static files including compiled JS are hosted on Github. This is set up like
described
[here](https://medium.com/linagora-engineering/deploying-your-js-app-to-github-pages-the-easy-way-or-not-1ef8c48424b7):

All the commands below assume that you already have a git project initialized and that you are in its root folder.

```
# Create an orphan branch named gh-pages
git checkout --orphan gh-pages
# Remove all files from staging
git rm -rf .
# Create an empty commit so that you will be able to push on the branch next
git commit --allow-empty -m "Init empty branch"
# Push the branch
git push origin gh-pages
```

Now that the branch is created and pushed to origin, let’s configure the worktree correctly:

```
# Come back to master
git checkout master
# Add dist to .gitignore
echo "dist/" >> .gitignore
git worktree add dist gh-pages
```

That’s it, you can now build your app as usual with npm run build . If you cd to
the dist folder, you will notice that you are now in the gh-pages branch and if
you go back to the root folder, you will go back to master .

To deploy to Github Pages:

```
cd dist
git add .
git commit -m "update build"
git push
```

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
