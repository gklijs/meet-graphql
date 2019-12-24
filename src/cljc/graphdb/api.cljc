(ns graphdb.api)

(defprotocol GraphDB
  (insert [this id data])
  (evict [this id])
  (set-property [this id property])
  (remove-property [this id property-type])
  (set-properties [this id properties])
  (remove-properties [this id properties])
  (update-property [this id property-update])
  (add-relation [this id relation])
  (remove-relation [this id relation])
  (add-relations [this id relations])
  (remove-relations [this id relations])
  (find-by-id [this id])
  (find-by-property [this property])
  (find-by-relation [this relation]))