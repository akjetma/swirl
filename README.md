# swirl

![Demonstration](http://i.imgur.com/67xA6PI.gif) 

## Why

- Mostly for fun
- Interactive remote demonstrations of ClojureScript concepts
- Technical interviews

Here are the initial problems I wanted to address with this thing:

- _Sandboxing_ - As far as I know, this is the only ClojureScript repl website thing that sandboxes its evaluation environment. So you can do things like `(set! js/document.body.innerHTML "hey")` without taking the repl input/interface along with it.
- _Collaboration_ - Collaborative coding environments like CoderPad support Clojure (evaluated server-side) but not Clojurescript.

Solving those problems required addressing a bunch of secondary ones as well.

- _Text Synchronization_ - Swirl implements the awesome text synchronization algorithm found in this [awesome paper](https://neil.fraser.name/writing/sync/) by Neil Fraser. I chose to do it this way because it is the coolest way. It is largely symmetrical between the server and client. Everybody's sending and receiving diffs with the server--all these patches swirling around, it's crazy. Initially I wanted to pull this functionality into a seperate library, but I had other yaks to shave and it was pretty tough to isolate a substantial enough amount of the process from the implementation details (websockets, 'storage') to warrant a seperate library.
- _Diff Match Patch_ - I made a cljsjs package for the Javascript version of Google's Diff Match Patch library, then wrapped the Java version and the cljsjs version into a symmetrical Clojure/Clojurescript library.
- _Iframe Shit_ - So much stuff around isolating the runtimes of the sandbox and interface environments. I ended up with three clojurescript builds--the client/interface (can use advanced compilation in production), the sandbox (optimizations simple in prod), and 'repl-libs' which is a dummy (output file is unused) optimizations none build used to generate source files for the sandbox to pull in. The sandbox and client can technically refer to values across their windows but it is unnatural and buggy. Letting two running applications reach into each other like that is weird, so they just serialize/deserialize messages to/from each other via the postMessage API.
- _Starting and Connecting Things_ - Everything has to start in the correct order and there are a bunch of asynchrony issues with waiting for websocket connection + messages , attaching the CodeMirror thing, and making sure the sandbox and client have established their communication loop. I ended up with a bastardized version of StuartSierra's component pattern.
- _UI/Usability_ - This is pretty consistently my favorite thing to do in personal projects. Just making things usable and somewhat attractive/inviting. I still don't know how to visually indicate that the editor and output display are resizable without mouseover/click or the above gif, though.

## How

Here's my diagram of swirl's implementation of Differential Sync.

![Diagram](https://docs.google.com/drawings/d/1Bols4eiixy9qeyJkXWtfP0KSIXD85Co3-4D49D2t2_k/pub?w=1890&h=4740)

## Where

[Here's my running public instance](http://swirl-app.herokuapp.com), but really its designed to be run/deployed privately by you (largely because I don't want to implement an identity/authorization system or pay for actual servers right now).

If you're going to use this for an interview, definitely run your own server and make sure you're familiar with this thing's faults and quirks.

### Heroku Instructions

1. Clone this repo
2. `heroku apps:create` in the project root
3. `git push heroku master`

### Otherwise

1. `lein uberjar`
2. `java $JVM_OPTS -cp target/swirl.jar clojure.main -m swirl.app.server`

#### or

1. `lein with-profile -dev cljsbuild once repl-libs sandbox client`
2. `lein run`

Starts on port specified by PORT environment variable, or 5000 by default.

#### or

1. `lein cljsbuild once repl-libs`
2. `rlwrap lein figwheel sandbox client`
3. `lein repl` 

