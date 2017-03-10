;;Copyright (c) 2017 Rafik Naccache <rafik@fekr.tech>

(ns postagga.core-test
  (:require [clojure.test :refer :all]
            [postagga.core :refer :all]
            [postagga.parser :refer [parse-tags-rules]]
            [postagga.tagger :refer [viterbi]]))


;; Here we force the use of a model, but in trainer.clj we have the
;; means to create such a model

(def sample-model ; as trained by train in trainer.clj
  {:states #{"P" "V" "N" "D"},
   :transitions {["P" "V"] 1.0, ["V" "D"] 1.0, ["D" "N"] 1.0},
   :emissions
   {["P" "Je"] 1.0,
    ["V" "Mange"] 0.5,
    ["V" "Tue"] 0.5,
    ["N" "Pomme"] 0.5,
    ["N" "Mouche"] 0.5,
    ["D" "Une"] 1.0},
   :init-probs {"P" 1.0}})

;; I can plug any pos-tagger, given a vector of words returns a vector or [w [pos-tags]]
;; Now we have a HMM based models, which is state of the art: the Viterbi Algorithm
(def sample-pos-tagger-fn (partial viterbi
                                   (:states sample-model)
                                   (:init-probs sample-model)
                                   (:transitions sample-model)
                                   (:emissions sample-model)))

;; TODO : create the tokenizer ns
(def sample-tokenizer-fn #(clojure.string/split % #"\s"))

(def sample-rules [{:id :sample-rule-0
                    :rule [:sujet
                           #{:get-value #{"P"}}

                           :action
                           #{:get-value #{"V"}}

                           :objet
                           #{#{"D"}}
                           #{:get-value #{"N"}}]}
                   
                   
                   {;;Rule 0 "Montrez moi les chaussures noires"
                    :id :sample-rule-1
                    :rule [:intent       ;;<----- A atep
                           #{:get-value #{"NPP"}}    ;;<----- A status in the parse machine (a set of possible sets of POS TAGS)
                           #{#{"NC"}}
                           
                           :product
                           #{#{"DET"}}
                           #{:get-value #{"NC"}} ;;<- I get this part of speech as a value, will find an entry :product ["Text"]

                           :qualif
                           #{:multi :get-value #{"ADJ"}}]} ;;<- multi: I can get several times this state
                   
                   {;;Rule 1 "Je cherche une montre analogique"
                    :id :sample-rule-2
                    :rule [:intent       ;;<----- A atep
                           #{#{"CLS"}}    ;;<----- A status in the parse machine (a set of possible sets of POS TAGS)
                           #{:get-value #{"V"}}
                           
                           :product
                           #{#{"DET"}}
                           #{:get-value #{"NC"}}
                           
                           :qualif
                           #{:multi :get-value #{"ADJ"}}]}])


(deftest sample-rules-pass
  (testing "Je tue une mouche doit retourner P V D N")
  (is (=  {:sujet["Je"] :action ["tue"], :objet ["pomme"]}
          (-> (parse-tags-rules sample-tokenizer-fn sample-pos-tagger-fn sample-rules "Je tue une pomme" [])
              (get-in [:result :data])))))