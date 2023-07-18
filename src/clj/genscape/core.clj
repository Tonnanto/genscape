(ns genscape.core
  (:require [quil.core :as q]
            [genscape.dynamic :as dynamic])
  (:gen-class))

(q/defsketch example
  :title "Genscape"
  :setup dynamic/setup
  :draw dynamic/draw
             ; :renderer :p2d
  :size [900 900])

(defn refresh []
  (use :reload 'genscape.dynamic)
  (.redraw example))

(defn get-applet []
  example)