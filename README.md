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
* [lucos media player](https://github.com/lucas42/lucos_media_player) (path of media player is currently hardcoded)
* [lucos media controller](https://github.com/lucas42/lucos_media_controller) (path of media controller is currently hardcoded)

## Test Dependencies
* [Junit5](https://junit.org/junit5/docs/current/user-guide/)
* [JaCoCo](https://www.jacoco.org/jacoco/trunk/index.html)
* Unzip (used for setting up jacoco)

## Setup (handled by Dockerfile)
There is a service file included to allow systemd to run it.
Alternatively, run `java -cp .:bin:../lib/java/* Manager $port` (assumes ../../lib/java/* points to where your gson library is)

The server requires a file named "config.properties" in the root of the project.  This should consist of newline separated key/value pairs (the key and value should be deliminated by an equal sign).  The following keys are used by the server:
* default_img
* default_thumb
* playlist

## Running
`nice -19 docker-compose up -d --no-build`

## Building
The build is configured to run in Dockerhub when a commit is pushed to the master branch in github.