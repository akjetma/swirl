# swirl

## What

![Demonstration](http://i.imgur.com/67xA6PI.gif) 

## Why

Needed it for job interviews.

## How

![Diagram of Differential Sync](https://docs.google.com/drawings/d/1Bols4eiixy9qeyJkXWtfP0KSIXD85Co3-4D49D2t2_k/pub?w=1890&h=4740)

## Where

[here's mine](http://swirl-app.herokuapp.com).

If you want to use this for an interview or something, I suggest running your own (private) server.

### Heroku instructions

1. Clone this repo
2. `heroku apps:create`
3. `git push heroku master`

### Otherwise

1. `lein uberjar`
2. `java $JVM_OPTS -cp target/swirl.jar clojure.main -m swirl.app.server`

#### or

1. `lein with-profile -dev cljsbuild once repl-libs sandbox client
2. `lein run`

starts on port specified by PORT environment variable, or 5000 by default

#### or

1. `lein cljsbuild once repl-libs`
2. `rlwrap lein figwheel sandbox client`
3. `lein repl` 

also starts on port PORT/5000