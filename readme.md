#lucos Media Manager
A java service for keeping track of which music is currently playing.

## Dependencies
* java
* gson
* [lucos media player](https://github.com/lucas42/lucos_media_player) (path of media player is currently hardcoded)
* [lucos media controller](https://github.com/lucas42/lucos_media_controller) (path of media controller is currently hardcoded)

## Running
The web server is designed to be run within lucos_services, but can be run standalone by running `java -cp .:bin:../../lib/java/* Manager $port $services_domain` (assuming ../../lib/java/* points to where your gson library is)
