var CLOSURE_UNCOMPILED_DEFINES = null;
if(typeof goog == "undefined") document.write('<script src="js/out_sb/goog/base.js"></script>');
document.write('<script src="js/out_sb/cljs_deps.js"></script>');

document.write("<script>if (typeof goog != \"undefined\") { goog.require(\"figwheel.connect\"); }</script>");
document.write('<script>if (typeof goog != "undefined") { goog.require("sandbox.core"); } else { console.warn("ClojureScript could not load :main, did you forget to specify :asset-path?"); };</script>');