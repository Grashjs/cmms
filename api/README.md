# Atlas CMMS API

This is the REST backend (Java8-Spring Boot) of the web
application.
We would be very happy to have new contributors join us.
**And please star the repo**.

## How to run locally ?

Set the environment variables not starting with `REACT_APP` from [here](../README.MD#set-environment-variables)

Without docker, you should first install and use JDK 8 then create a Postgres database. After that go
to [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml), change the url, username and
password.

```shell
mvn spring-boot:run
```