(ns swirl.common.env
  (:require [environ.core :as environ]))

(defmacro env 
  [k]
  (environ/env k))
