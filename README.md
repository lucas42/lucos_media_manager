#lucos Media Manager
A java service for keeping track of which music is currently playing.

## Dependencies
* docker
* docker-compose

## Build-time Dependencies
* Java
* Make
* wget
* [Google gson](https://code.google.com/p/google-gson/)

## Test Dependencies
* [Junit5](https://junit.org/junit5/docs/current/user-guide/)
* [JaCoCo](https://www.jacoco.org/jacoco/trunk/index.html)
* Unzip (used for setting up jacoco)

## Setup (handled by Dockerfile)
There is a service file included to allow systemd to run it.
Alternatively, run `java -cp .:bin:../lib/java/* Manager $port` (assumes ../../lib/java/* points to where your gson library is)

## Running
`nice -19 docker-compose up -d --no-build`

## Building
The build is configured to run in Dockerhub when a commit is pushed to the main branch in github.