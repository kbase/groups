# KBase Groups TODOs


* Add a docker-compose.yml that starts the service

* This really applies to most of the java services, and especially the HTTP/RESTish services -
  there's a bunch of very similar or identical code across the services that really should
  be packaged into a common jar for reuse rather than having very similar code in many repos.
  My experiences with doing this with java-common have been somewhat unpleasant so I've been
  hesitant to deal with it. It'd be easier if we gradleized our builds first.
  * Pushing the jar into a repo on a successful build would help
  * Particular items:
    * DB schema version checking (many java services)
      * UJS has some update code as well
    * The mongo duplicate key exception checking code
    * UI logging & exception handling stuff (HTTP/RESTish)
    * ErrorType enum and exception hierarchy
    * Null checking and string checking in utils classes
    * The Name class which is everywhere
    * Class loading helper methods in auth and assembly homology
    * The call id handling really needs to be separated from the logger in the HTTP
      services so it'd be easier to swap out the logger rather than use the
      JsonServerSyslog class.
    * The configuration classes have quite a bit in common - could add a MongoConfig
      helper class for example.
    * MongoStorageTestManger
    * TestCommon
    * The workspace controller in assembly homology & this repo
  * It'd be nice to use a separate repo from java-common as that's got so much cruft in it
    and the documentation and testing is almost non-existent