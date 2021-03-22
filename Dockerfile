FROM openjdk:16-alpine

WORKDIR /web/lucos/lucos_media_manager

# Other repos which need installed alongside media manager.  Ideally, there would be proper dependency management here, or the player/controller would be served from their own containers.
RUN apk add git
RUN git clone https://github.com/lucas42/lucos_media_player.git /web/lucos/lucos_media_player
RUN git clone https://github.com/lucas42/lucos_media_controller.git /web/lucos/lucos_media_controller

RUN apk add make

COPY . .
RUN make build

ENV PORT 8080
EXPOSE $PORT

CMD [ "java", "-cp", ".:bin:libs/*", "Manager" ]