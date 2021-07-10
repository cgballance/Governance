# EnforcerServices

### Open API
The api that is exposed to angular web application or any other caller is defined in:
* src/main/resources/public/governance.yaml

The file is placed in an area that will be accessible to unauthenticated users.  Also, the API is
conveyed to users via the ReDoc library, which is referenced in:
* src/main/resources/public/api.html

ReDoc makes it really simple to show what the api looks like.  The API is segregated into two different types:
* Governance State
* Build State  

*Governance State* is concerned with the act of managing libraries.  
*Build State* is concerned with the development community's interaction/usage with the mandated Governance policies
embodied in the *Governance State*.

The *src/main/resources/public/governance* directory is populated with a version of the EnforcerWeb Angular application, which
provides the administrative view of the system.  Copy the *dist* directory items from EnforcerWeb into that directory to enable the
ability to run both the API Services and the Administrative application from the same web server.

### Database Setup
*database.schema* has been tested with PostgreSQL.  It should require very little amount of changes to work with other Relational databases.
No, you can't put it into Cassandra, MongoDB, HBase, etc.  This is old school.
Once you've managed to install the database schema, set up the Spring Boot application.properties file appropriately, providing the JDBC related
information as appropriate.

*TODO* ER-DIAGRAM

### Transport Layer Security(TLS/SSL)
This is a Spring Boot Java application.  You will need to create your own governance.keystore or whatever name you choose.  Just change the reference to
the filename in the application.properties, along with key alias, store password, whatever other ssl properties you see in the file.

### API Security
The API is protected through the usage of Spring Security annotations for role declarations necessary for access to any api call.
If you wish to use an API gateway, you'll have to see what support they have for the OpenAPI 2.0 specification, as the roles are declared there as well
as the annotations.  Unfortunately, the openapi plugin that is used to generate the API stubs does not know what to do with this information.  They
are currently matched through eyeball technology :(

At some point, i may dive into the openapi maven plugin and do the code.  The delegate interfaces generated just need the Role annotations propagated, which will then
automatically be adhered to by the Service implementation that is written(AOP-like!).  For now this is just fyi.

The authorization information[caller roles] is extracted from a JWT Bearer Token.  It is placed into the JWT when the user successfully authenticates via a
call to the /v1/authentication API call, which is currently a simple login/password.  This could/should be changed as appropriate to link into your IDAM system of choice.  In any case, fill out an industry standard JWT and you'll be A-OK.
The code for dealing with authentication and JWT setup is located in:
*com.webforged.enforcer.management.security.jwt

FOR POC TESTING PURPOSES, THE BUILT-IN LOGIN is *chas* and the password is *chas*.  This is just demo-ware currently.  If there's interest in using
this, then you'll be linking the code to your IDAM, so not really important right now.

How does the security work?  There is a servlet filter that every request visits.  If the security configuration doesn't require any special access, then
the request continues on, unimpeded.  Accessing the https://<api services>/api.html is an example of a freely accessible URL.
See:
* com.webforged.enforcer.management.security.jwt.JwtWebSecurityConfig::configure method



#### Developer Notes
Changes are first made to the governance.yaml API specification.  Executing *maven install* will, if the api is properly defined, result in
java files being produced in target/generated-sources/openapi/src/generated/java.  Methods that are created in the io.reflectoring.api.*Delegate.java
files are what are copied into the com.webforged.enforcer.management.services appropriate file for implementation.  Annotations are used for dealing with security roles, so check out the examples.  Typically, all Build State related methods will only have Read access.  It would be bad to let editing take place on those things.  Your build toolchain(s) should be the only thing(s) that populate the build related data in the system.  There are two
examples that specify what is needed to make this happen.  Maven and Gradle plugins were developed to interface with the database.  The TestProject contains implementation for both that show how they interact.  Applications should NOT know about these plugins.  DevOps procedures to modify application pom.xml or build.gradle as required should be developed.

#### TODO
- DevOps procedures to modify application pom.xml and build.gradle

- build_items table additional column to indicate infraction (boolean) /* build table has a varchar infraction that holds all info right now */
This will make it easier to view the errand artifacts being used.

- WARNING for each deprecated artifact usage


