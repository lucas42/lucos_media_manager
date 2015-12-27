#lucos Media Manager
A java service for keeping track of which music is currently playing.

## Dependencies
* Java
* [Google gson](https://code.google.com/p/google-gson/)
* [lucos media player](https://github.com/lucas42/lucos_media_player) (path of media player is currently hardcoded)
* [lucos media controller](https://github.com/lucas42/lucos_media_controller) (path of media controller is currently hardcoded)

##Installation
To build the project, run *./build.sh*

## Running
The web server is designed to be run within lucos_services, but can be run standalone by running `java -cp .:bin:../lib/java/* Manager $port $services_domain` (assuming ../../lib/java/* points to where your gson library is)

The server requires a file named "config.properties" in the root of the project.  This should consist of newline separated key/value pairs (the key and value should be deliminated by an equal sign).  The following keys are used by the server:
* default_img
* default_thumb
* edit_url
* playlist
* vlcweb 
