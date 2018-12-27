(ns ipogudin.collie.client.db)

(def default-db
  {:name "collie"})

(comment
{
 :schema {} ; a schema describing all entities and their relations
 :selecting {
             :status #{:sync :unsync} ; whether selected entities are received from the server side or not
             :entities nil ; a vector with entities being selected
             :type     type ; a type of entities being selected
             :pagination {:page 1 :limit 1}
             :ordering {:field-name :asc}
             :filtering {:field-name "value"}
             }
 :editing {
           :status #{:sync :unsync} ; whether dependencies are received from the server side or not
           :entity {}; an entity which is being edited right now
           :command-id-to-dep-field {}; mapping of command id to dependency field name. Those commands request all option values for dependencies.
           }
 })