FROM openjdk:13-alpine

WORKDIR /web/lucos/lucos_media_manager

# Other repos which need installed alongside media manager.  Ideally, there would be proper dependency management here, or the player/controller would be served from their own containers.
RUN apk add git
RUN git clone https://github.com/lucas42/lucos_media_player.git /web/lucos/lucos_media_player
RUN git clone https://github.com/lucas42/lucos_media_controller.git /web/lucos/lucos_media_controller

RUN mkdir -p /web/lucos/lib/java/

RUN wget "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.1/gson-2.8.1.jar" -O /web/lucos/lib/java/gson-2.8.1.jar

COPY . .

RUN ./build.sh

ENV PORT 8080
EXPOSE $PORT

CMD [ "java", "-cp", ".:bin:../lib/java/*", "Manager" ]