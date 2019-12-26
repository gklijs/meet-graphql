(ns graphdb.in-memory
  (:require [clojure.set :refer [union difference]]
            [com.stuartsierra.component :as component]
            [graphdb.api :as api]
            [taoensso.timbre :refer [log trace debug info warn error fatal report
                                     logf tracef debugf infof warnf errorf fatalf reportf
                                     spy get-env]]))

(defn- add-single-to-index
  [index id type value]
  (if (coll? value)
    index
    (let [new-ids (set (conj (get-in index [type value]) id))]
      (assoc-in index [type value] new-ids))))

(defn- add-to-index
  [index id type value]
  (if (and (coll? value) (not (map? value)))
    (reduce (fn [i v] (add-single-to-index i id type v)) index value)
    (add-single-to-index index id type value)))

(defn- remove-single-from-index
  [index id type value]
  (if (coll? value)
    index
    (let [new-ids (disj (get-in index [type value]) id)]
      (if (empty? new-ids)
        (update index type #(dissoc % value))
        (assoc-in index [type value] new-ids)))))

(defn- remove-from-index
  [index id type value]
  (if (and (coll? value) (not (map? value)))
    (reduce (fn [i v] (remove-single-from-index i id type v)) index value)
    (remove-single-from-index index id type value)))

(defn- initial-index
  "build the initial index based on the content"
  [initial-content]
  (reduce-kv (fn [index id properties]
               (reduce-kv (fn [i type value] (add-to-index i id type value)) index properties)) {} initial-content))

(defn- add-single-relation-f
  "add an array with the new value, or add the new value to the existing array"
  [item relation-type id]
  (if
    (contains? item relation-type)
    (update item relation-type conj id)
    (assoc item relation-type #{id})))

(defn- add-multiple-relations-f
  "add an array with the new value, or add the new value to the existing array"
  [item relation-type ids]
  (if
    (contains? item relation-type)
    (update item relation-type union ids)
    (assoc item relation-type ids)))

(defn- add-relation-f
  "add one or  multiple relationships"
  [item relation-type id-or-ids]
  (if (set? id-or-ids)
    (add-multiple-relations-f item relation-type id-or-ids)
    (add-single-relation-f item relation-type id-or-ids)))

(defn- remove-single-relation-f
  "remove the array with one value, or remove value to the existing array"
  [item relation-type value current-set]
  (let [new-relations (disj current-set value)]
    (if (empty? new-relations)
      (dissoc item relation-type)
      (assoc item relation-type new-relations))))

(defn- remove-multiple-relations-f
  "remove the array with one value, or remove value to the existing array"
  [item relation-type values current-set]
  (let [new-relations (difference current-set values)]
    (if (empty? new-relations)
      (dissoc item relation-type)
      (assoc item relation-type new-relations))))

(defn- remove-relation-f
  "remove the array with one value, or remove value to the existing array"
  [item relation-type value]
  (if-let [current-set (relation-type item)]
    (if
      (set? value)
      (remove-multiple-relations-f item relation-type value current-set)
      (remove-single-relation-f item relation-type value current-set))
    item))

(defrecord InMemoryGraphDB [initial-content]
  component/Lifecycle
  (start [this]
    (info "starting with" (count (keys (:initial-content this))) "initial items")
    (-> this
        (assoc :index (atom (initial-index (:initial-content this))))
        (assoc :content (atom (:initial-content this)))))
  (stop [_])
  api/GraphDB
  (insert [this id data]
    (swap! (:content this) assoc id data)
    (doseq [[type value] data] (swap! (:index this) add-to-index id type value))
    id)
  (evict [this id]
    (let [[old _] (swap-vals! (:content this) dissoc id)]
      (doseq [[type value] (id old)] (swap! (:index this) remove-from-index id type value))
      (contains? old id)))
  (set-property [this id [property-type value]]
    (let [new-state (swap! (:content this) update id #(assoc % property-type value))]
      (swap! (:index this) add-to-index id property-type value)
      (id new-state)))
  (remove-property [this id property-type]
    (if (contains? @(:content this) id)
      (let [[old new] (swap-vals! (:content this) update id #(dissoc % property-type))]
        (swap! (:index this) remove-from-index id property-type (property-type old))
        (id new))
      false))
  (set-properties [this id properties]
    (let [[old new] (swap-vals! (:content this) update id #(merge % properties))]
      (doseq [type (keys properties)] (swap! (:index this) remove-from-index id type (type old)))
      (doseq [[type value] properties] (swap! (:index this) add-to-index id type value))
      (id new)))
  (remove-properties [this id properties]
    (if (contains? @(:content this) id)
      (let [[old new] (swap-vals! (:content this) update id #(apply dissoc % properties))]
        (doseq [property properties] (swap! (:index this) remove-from-index id property (property old)))
        (id new))
      false))
  (update-property [this id [property-type update-f]]
    (when (get-in @(:content this) [id property-type])
      (let [[old new] (swap-vals! (:content this) update-in [id property-type] update-f)]
        (swap! (:index this) remove-from-index id property-type (property-type old))
        (swap! (:index this) add-to-index id property-type (property-type new))
        (id new))))
  (add-relation [this id [relation-type value]]
    (let [new-state (swap! (:content this) update id #(add-relation-f % relation-type value))]
      (swap! (:index this) add-to-index id relation-type value)
      (id new-state)))
  (remove-relation [this id [relation-type value]]
    (if (contains? @(:content this) id)
      (let [new-state (swap! (:content this) update id #(remove-relation-f % relation-type value))]
        (swap! (:index this) remove-from-index id relation-type value)
        (id new-state))
      false))
  (add-relations [this id relations]
    (let [new-state (swap! (:content this) update id #(reduce-kv add-relation-f % relations))]
      (doseq [[relation-type value] relations] (swap! (:index this) add-to-index id relation-type value))
      (id new-state)))
  (remove-relations [this id relations]
    (if (contains? @(:content this) id)
      (let [new-state (swap! (:content this) update id #(reduce-kv remove-relation-f % relations))]
        (doseq [[relation-type value] relations] (swap! (:index this) remove-from-index id relation-type value))
        (id new-state))
      false))
  (find-by-id [this id]
    (id @(:content this)))
  (select-by-id [this id keys]
    (select-keys (id @(:content this)) keys))
  (find-by-property [this [property-type value]]
    (get-in @(:index this) [property-type value]))
  (find-by-relation [this [relation-type value]]
    (get-in @(:index this) [relation-type value])))

(defn -main [& args]
  (let [system (component/system-map :db (InMemoryGraphDB. {:user/richhickey {:name "Rich"}}))
        system (component/start-system system)]
    (spy :info (api/insert (:db system) :user/gklijs {:user/name "Gerard" :user/age 36}))
    (spy :info (api/update-property (:db system) :user/gklijs [:user/age inc]))
    (spy :info (component/stop-system system))))
