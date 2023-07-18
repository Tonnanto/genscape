(ns genscape.dynamic
  (:require [clojure.java.shell :refer [sh]]
            [genartlib.algebra :refer [point-angle angular-coords avg]]
            [genartlib.geometry :refer :all]
            [genartlib.random :refer [gauss odds]]
            [genartlib.util :refer [set-color-mode w h]]
            [quil.core :as q]))

; the setup function is only called once by Processing during startup
(defn setup
  []
  (q/smooth)
  ; avoid some saving issues
  (q/hint :disable-async-saveframe)

  ; create a directory for storing versioned code
  ; This uses "bash -c" to support my WSL setup, but if on Linux or Mac
  ; you could just call mkdir directly
  (sh "bash" "-c" "mkdir versioned-code"))

(declare actual-draw)

; the draw function will be called every time we refresh (i.e. reload the code
; and re-execute it)
(defn draw
  []
  ; disable animation, just draw one frame
  (q/no-loop)

  ; set color space to HSB with hue in [0, 360], saturation in [0, 100],
  ; brightness in [0, 100], and alpha in [0.0, 1.0]
  (set-color-mode)

  ; make it easy to generate multiple images
  (doseq [img-num (range 200)]
    (let [cur-time (System/currentTimeMillis)
          ; Grab the current time in nanos to use for a seed. This guarantees
          ; that we get a new seed every run, and it also increases every time
          ; (unless we restart the process)
          seed (System/nanoTime)]

      ; only save the versioned code the first time in this loop (because the code
      ; will never change inside of this loop)
      (when (zero? img-num)
        (let [code-dirname (str "versioned-code/" seed)]
          ; only copy source files, to avoid duplicating temporary files opened
          ; by vim, etc. Also zip them, since they will rarely be read, compression
          ; ratio is good, and the number of total files will stay lower
          (sh "bash" "-c" (str "zip -r " code-dirname " src -i '*.clj' '*.java'"))))

      (println "setting seed to:" seed)
      (q/random-seed seed) ; used by Processing for most random functions
      (q/noise-seed seed) ; used by Processing for perlin noise

      (try
        (actual-draw)
        (catch Throwable t
          (println "Exception in draw function:" t)))

      (println "gen time:" (/ (- (System/currentTimeMillis) cur-time) 1000.0) "s")
      (let [img-filename (str "output/" "img-" img-num "-" cur-time "-" seed ".png")]
        (q/save img-filename)
        (println "done saving" img-filename)

        ; Some part of image saving appears to be async on Windows. This is lame, but
        ; for now, add a sleep to help avoid compressing partially-written files.
        ; (Thread/sleep 500)

        ; The 'convert' command comes from ImageMagick. By default, processing will save
        ; un-compressed tif files, which tend to be quite large. This applies LZW compression,
        ; which is lossless and reasonably compact. This command needs to be available on the
        ; command line for this to be successful.
        (let [convert-cmd (str "convert -compress lzw " img-filename " " img-filename)
              results (sh "bash" "-c" convert-cmd)]
          (if (zero? (:exit results))
            (println "done compressing")
            (println "WARNING: compression failed."
                     "Command was: bash -c '" convert-cmd "'; err:" (:err results))))))))

; HELPER
; (defn interpolate-angle
;   [angle1 angle2 t mode] 
;   (case mode
;     :radians (if (> (q/abs (- angle1 angle2)) q/PI)
;                (+ q/PI (+ (* angle1 (- 1 t)) (* angle2 t)))
;                (+ (* angle1 (- 1 t)) (* angle2 t)))
;     :degrees (if (> (q/abs (- angle1 angle2)) 180)
;               (+ 180 (+ (* angle1 (- 1 t)) (* angle2 t)))
;               (+ (* angle1 (- 1 t)) (* angle2 t))))
;   )

(defn interpolate-angle [angle1 angle2 t]
  (let [shortest-angle (mod (- angle2 angle1) 360)
        step (- (mod (* 2 shortest-angle) 360) shortest-angle)
        interpolated-angle (mod (+ angle1 (* step t)) 360)]
    interpolated-angle))

(defn interpolate-color
  [c1 c2 t]
  (vector
   (interpolate-angle (get c1 0) (get c2 0) t)
   (+ (* (get c1 1) (- 1 t)) (* (get c2 1) t))
   (+ (* (get c1 2) (- 1 t)) (* (get c2 2) t))))

(def sun-rise 5)
(def sun-set 20)
(def time-values [{:center-color [241 19 84] :sky-color [235 87 20]  :center-height 1   :center-shine [241 19 84] :center-blend 0.05} ; 0 moon
                  {:center-color [234 19 89] :sky-color [236 76 32]  :center-height 0.8 :center-shine [234 19 89] :center-blend 0.05} ; 1
                  {:center-color [250 11 89] :sky-color [237 64 47]  :center-height 0.6 :center-shine [250 11 89] :center-blend 0.05} ; 2
                  {:center-color [275 7 90]  :sky-color [258 56 46]  :center-height 0.4 :center-shine [275 7 90] :center-blend 0.05}  ; 3
                  {:center-color [47 13 95]  :sky-color [275 45 52]  :center-height 0.2 :center-shine [47 13 95] :center-blend 0.1}  ; 4 moon   

                  {:center-color [0 56 93]   :sky-color [190 39 93]  :center-height 0   :center-shine [0 56 93] :center-blend 0.6} ; 5 sun rise 
                  {:center-color [18 56 95]  :sky-color [195 50 88]  :center-height 0.2 :center-shine [330 56 95] :center-blend 0.4}  ; 6
                  {:center-color [33 50 99]  :sky-color [201 53 83]  :center-height 0.4 :center-shine [290 20 100] :center-blend 0.3}  ; 7
                  {:center-color [48 53 98]  :sky-color [207 59 78]  :center-height 0.6 :center-shine [250 15 100] :center-blend 0.2}  ; 8
                  {:center-color [64 46 100] :sky-color [213 67 73]  :center-height 0.8 :center-shine [200 0 100] :center-blend 0.15} ; 9 sun bright
                  {:center-color [64 46 100] :sky-color [218 74 68]  :center-height 1   :center-shine [200 0 100] :center-blend 0.15} ; 10
                  {:center-color [64 46 100] :sky-color [200 80 100] :center-height 1   :center-shine [200 0 100] :center-blend 0.15} ; 11
                  {:center-color [64 46 100] :sky-color [200 80 100] :center-height 1   :center-shine [200 0 100] :center-blend 0.15} ; 12
                  {:center-color [64 46 100] :sky-color [200 80 100] :center-height 1   :center-shine [200 0 100] :center-blend 0.15} ; 13
                  {:center-color [64 46 100] :sky-color [218 74 68]  :center-height 1   :center-shine [200 0 100] :center-blend 0.15} ; 14
                  {:center-color [64 46 100] :sky-color [213 67 73]  :center-height 0.8 :center-shine [200 0 100] :center-blend 0.15} ; 15 sun bright
                  {:center-color [60 35 98]  :sky-color [207 59 78]  :center-height 0.6 :center-shine [200 0 100] :center-blend 0.2}  ; 16 
                  {:center-color [60 24 99]  :sky-color [201 53 83]  :center-height 0.4 :center-shine [200 0 100] :center-blend 0.3}  ; 17
                  {:center-color [55 12 100] :sky-color [300 30 100]  :center-height 0.2 :center-shine [330 56 95] :center-blend 0.4}  ; 18
                  {:center-color [0 0 100]   :sky-color [50 50 100]  :center-height 0   :center-shine [0 56 93] :center-blend 0.6} ; 19 sun set 

                  {:center-color [275 13 90]  :sky-color [275 45 42]  :center-height 0.2 :center-shine [275 13 85] :center-blend 0.15}  ; 20 moon
                  {:center-color [258 7 83]   :sky-color [258 56 39]  :center-height 0.4 :center-shine [258 7 83] :center-blend 0.1}  ; 21
                  {:center-color [237 11 81]  :sky-color [237 64 32]  :center-height 0.6 :center-shine [237 7 83] :center-blend 0.1} ; 22
                  {:center-color [236 19 78]  :sky-color [236 76 26]  :center-height 0.8 :center-shine [236 7 83] :center-blend 0.1} ; 23
                  {:center-color [235 19 74]  :sky-color [235 87 20]  :center-height 1   :center-shine [235 7 83] :center-blend 0.1}]) ; 24 moon
(def ground-colors [[15 52 51]
                    [14 27 59]
                    [22 20 76]
                    [85 62 88]
                    [71 60 62]])
                  

(defn pick-color
  [time key]
  (let [c1 (get-in time-values [(int time) key])
        c2 (get-in time-values [(inc (int time)) key])]
    (interpolate-color c1 c2 (- time (int time)))))

; (defn pick-color
;   [time key]
;   (get-in time-values [(int time) key]))

(defn pick-sky-color
  [time variance center-blend]
  (let [base-color (pick-color time :sky-color)
        adj-color (interpolate-color (pick-color time :center-shine) base-color (min 1 center-blend))]
    (vector (gauss (get adj-color 0) variance) (+ (q/random -10 10) (get adj-color 1)) (+ (q/random 0 10) (get adj-color 2)))))

(defn pick-foreground-color
  ([time variance]
   (let [base-color (rand-nth ground-colors)]
   (vector (gauss (get base-color 0) variance) (+ (q/random -10 10) (get base-color 1)) (+ (q/random 0 10) (get base-color 2)))))
  ([base-color time variance distance]
   (let [adj-color (interpolate-color base-color (pick-color time :sky-color) (min 1 (* 0.4 distance)))]
     (vector (gauss (get adj-color 0) variance) (+ (get adj-color 1) (q/random (* distance -30) 0)) (+ (get adj-color 2) (q/random 0 (* distance 30)))))))
  

; DRAW BACKGROUND
(def avg-center-sections-count 32)
(def avg-poly-count 32)
(defn draw-background
  [time horizon]
  (let [diam (w (gauss 0.15 0.025))
        color (pick-color time :center-color)
        x (w (q/random 0.3 0.7))
        y (+ (* diam 0.7) (* horizon (- 1 (avg (get-in time-values [(int time) :center-height]) (get-in time-values [(inc (int time)) :center-height])))))
        section-count (int (gauss avg-center-sections-count 8))
        first-section-angle (q/random 0 (* 2 q/PI))
        angle-step (/ (* 2 q/PI) section-count)
        sky-color-variance (gauss 10 6)]

    (q/fill color)
    (q/ellipse x y diam diam)

    (doseq [i (range section-count)]
      (let [angle (+ first-section-angle (* i angle-step))
            next-angle (+ first-section-angle (* (inc i) angle-step))
            line-start (angular-coords x y angle (* 0.5 diam))
            next-line-start (angular-coords x y next-angle (* 0.5 diam))
            poly-count (int (gauss avg-poly-count 6))
            line-end (angular-coords x y angle (w 1))
            next-line-end (angular-coords x y next-angle (w 1))]

        (doseq [poly-index (range poly-count)]
          (let [step1 (if (= poly-index 0) 0 (max 0 (w (gauss (* poly-index (/ 1 poly-count)) (/ 1 poly-count 2)))))
                step2 (if (= poly-index 0) 0 (max 0 (w (gauss (* poly-index (/ 1 poly-count)) (/ 1 poly-count 2)))))
                point1 (angular-coords x y angle (+ step1 (* 0.5 diam)))
                point2 (angular-coords x y next-angle (+ step2 (* 0.5 diam)))
                center-distance (w (* (+ 1 poly-index) (/ 1 poly-count)))
                sky-color-center-blend (/ center-distance (w (get-in time-values [(int time) :center-blend])))
                color (pick-sky-color time sky-color-variance sky-color-center-blend)]
            (q/fill color)
            (q/quad (get point1 0) (get point1 1) (get point2 0) (get point2 1) (get next-line-end 0) (get next-line-end 1) (get line-end 0) (get line-end 1))))))))

(defn draw-foreground
  [time horizon]
  
  (let [layer-count (+ 2 (rand-int 2))
        layer-type (if (odds 0.5) :straight :mountainy)
        hilliness (gauss 0.05 0.03)
        layer-color-variance (gauss 10 6)]

    (doseq [layer-index (range layer-count)]
      (let [layer-center-x (w (q/random -0.3 1.3))
            layer-center-y (+ horizon (h (+ (gauss 3 0.4))))
            layer-height (- layer-center-y (+ horizon (h (* layer-index (gauss 0.15 0.075)))))
            layer-distance (- 1 (/ layer-index (dec layer-count)))
            section-count (int (* (+ layer-distance 0.5) (gauss 24 8)))
            section-heights (into [] (for [x (range section-count)] (gauss layer-height (h (* hilliness (- 1.5 layer-distance))))))
            section-start-angle (point-angle [layer-center-x layer-center-y] [(w -0.2) (h 1.2)])
            section-end-angle (point-angle [layer-center-x layer-center-y] [(w 1.2) (h 1.2)])
            angle-step (/ (- section-end-angle section-start-angle) section-count)
            layer-base-color (pick-foreground-color time 30)]
        
        (doseq [i (range section-count)]
          (let [angle (+ section-start-angle (* i angle-step))
                next-angle (+ section-start-angle (* (inc i) angle-step))
                poly-count (int (gauss 64 6))
                line-end (angular-coords layer-center-x layer-center-y angle (get section-heights i))
                next-line-end (angular-coords layer-center-x layer-center-y next-angle (get section-heights (mod (inc i) section-count)))]
        
            (doseq [poly-index (range poly-count)]
              (let [step1 (case poly-index
                            0 0
                            (- poly-count 1) (get section-heights i)
                            (min (get section-heights i) (max 0 (gauss (* poly-index (/ layer-height poly-count)) (/ layer-height poly-count 2)))))
                    step2 (case poly-index
                            0 0
                            (- poly-count 1) (get section-heights (inc i) section-count)
                            (min (get section-heights (inc i) section-count) (max 0 (gauss (* poly-index (/ layer-height poly-count)) (/ layer-height poly-count 2)))))
                    point1 (angular-coords layer-center-x layer-center-y angle step1)
                    point2 (angular-coords layer-center-x layer-center-y next-angle step2)
                    color (pick-foreground-color layer-base-color time layer-color-variance layer-distance)]
                (q/fill color)
                (q/quad (get point1 0) (get point1 1) (get point2 0) (get point2 1) (get next-line-end 0) (get next-line-end 1) (get line-end 0) (get line-end 1))))))))))
        
  

(defn minutes-to-readable-time [minutes]
  (let [hours (quot minutes 60)
        mins (mod minutes 60)]
    (str (int hours) ":" (int mins))))

; MAIN DRAW
(defn actual-draw
  []
  ; Art goes here. For example:
  (q/background 40 2 98) ; off-white background
  (q/stroke-weight (w 0.002)) ; line thickness is 0.1% of the image width

  (let [time (q/random 0 24)
        sun? (and (>= time sun-rise) (< time sun-set))
        horizon (h (gauss 0.6 0.1))]

    (println "It is " (minutes-to-readable-time (* time 60)))

    (if sun? (q/stroke 0 0 100) (q/stroke 0 0 5))

    (draw-background time horizon)

    (q/stroke 0 0 5)

    (draw-foreground time horizon)))