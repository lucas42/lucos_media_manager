# lucos Media Manager
A java service for keeping track of which music is currently playing.

## Dependencies
* docker
* docker-compose

## Build-time Dependencies
* Java
* Maven
* [Google gson](https://code.google.com/p/google-gson/) - installed by Maven

## Test Dependencies
* [Junit5](https://junit.org/junit5/docs/current/user-guide/) - installed by Maven
* [JaCoCo](https://www.jacoco.org/jacoco/trunk/index.html) - installed by Maven
* [Mockito](https://site.mockito.org/) - installed by Maven

## Running
`docker compose up --build`

## Running tests locally
`mvn clean test`
