## Meet GraphQL

Not sure where this will be going, but the idea is to test/implements several Graph databases and use them with GraphQL.
So far it contains a `GrpahDB-api` withc is currently implemented in pure Clojure only.
Direction for now is to have multiple implementations, starting with PostgreSQL. Then seed the database using the generic
api, use Graphile and/or Hasura. Then continue to do the same with Graph Databases like Dgraph, FaunaDB and Neo4J

### Tests

Unit tests run by `./bin/kaocha.sh unit` features will run several database implementations and thus require some setup.