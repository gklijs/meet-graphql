(ns graphdb.test-api
  (:require [clojure.test :refer :all]
            [graphdb.api :as api]))

(def initial-state {:user/richhickey {:user/name "Rich"}})

(defn run-api-tests
  "runs a lot of tests on the supplied db"
  [db]
  (eval `(deftest ~(symbol "test-find-by-id")
           (is (= ~(api/find-by-id db :user/richhickey) {:user/name "Rich"}))))
  (eval `(deftest ~(symbol "test-insert")
           (is (= ~(api/insert db :user/gklijs #:user{:name "Gerard"}) :user/gklijs))))
  (eval `(deftest ~(symbol "test-set-property-existing-user")
           (is (=
                 ~(api/set-property db :user/gklijs [:user/last-name "Klijs"])
                 #:user{:name "Gerard", :last-name "Klijs"}))))
  (eval `(deftest ~(symbol "test-set-property-new-user")
           (is (=
                 ~(api/set-property db :user/cklijs [:user/last-name "Klijs"])
                 #:user{:last-name "Klijs"}))))
  (eval `(deftest ~(symbol "test-set-properties")
           (is (=
                 ~(api/set-properties db :user/gklijs #:user{:age 36, :gender :male})
                 #:user{:name "Gerard", :last-name "Klijs", :age 36, :gender :male}))))
  (eval `(deftest ~(symbol "test-update-property")
           (is (=
                 ~(api/update-property db :user/gklijs [:user/age inc])
                 #:user{:name "Gerard", :last-name "Klijs", :age 37, :gender :male}))))
  (eval `(deftest ~(symbol "test-update-property-when-it-does-not-exist")
           (is (= ~(api/update-property db :user/gklijs [:user/foo inc]) nil))))
  (eval `(deftest ~(symbol "test-remove-property")
           (is (=
                 ~(api/remove-property db :user/gklijs :user/age)
                 #:user{:name "Gerard", :last-name "Klijs", :gender :male}))))
  (eval `(deftest ~(symbol "test-add-relation")
           (is (=
                 ~(api/add-relation db :user/gklijs [:user/knows :user/richhickey])
                 #:user{:name "Gerard", :last-name "Klijs", :gender :male, :knows #{:user/richhickey}}))))
  (eval `(deftest ~(symbol "test-remove-properties")
           (is (=
                 ~(api/remove-properties db :user/gklijs [:user/gender :user/last-name])
                 #:user{:name "Gerard", :knows #{:user/richhickey}}))))
  (eval `(deftest ~(symbol "test-remove-relation")
           (is (=
                 ~(api/remove-relation db :user/gklijs [:user/knows :user/richhickey])
                 #:user{:name "Gerard"}))))
  (eval `(deftest ~(symbol "test-add-relations")
           (is (=
                 ~(api/add-relations db :user/gklijs {:user/knows #{:user/cklijs :user/richhickey :user/mmouse}, :work/employer :work/open-web})
                 #:user{:name "Gerard", :user/knows #{:user/cklijs :user/richhickey :user/mmouse}, :work/employer #{:work/open-web}}))))
  (eval `(deftest ~(symbol "test-remove-relations")
           (is (=
                 ~(api/remove-relations db :user/gklijs {:user/knows :user/mmouse, :work/employer :work/open-web})
                 #:user{:name "Gerard", :user/knows #{:user/cklijs :user/richhickey}}))))
  (eval `(deftest ~(symbol "test-find-by-property")
           (is (= ~(api/find-by-property db [:user/name "Gerard"]) #{:user/gklijs}))))
  (eval `(deftest ~(symbol "test-find-by-relation")
           (is (= ~(api/find-by-relation db [:user/knows :user/cklijs]) #{:user/gklijs}))))
  (eval `(deftest ~(symbol "test-evict-present")
           (is (= ~(api/evict db :user/gklijs) true))))
  (eval `(deftest ~(symbol "test-evict-absent")
           (is (= ~(api/evict db :user/gklijs) false))))
  (eval `(deftest ~(symbol "test-find-by-when-evicted")
           (is (= ~(api/find-by-id db :user/gklijs) nil))))
  (eval `(deftest ~(symbol "test-index-cleaned")
           (is (= ~(api/find-by-property db [:user/name "Gerard"]) nil))))
  (eval `(deftest ~(symbol "test-removed-in-index")
           (is (= ~(api/find-by-relation db [:user/knows :user/cklijs]) nil)))))

