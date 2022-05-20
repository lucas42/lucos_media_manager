FROM openjdk:18-alpine

WORKDIR /web/lucos/lucos_media_manager

RUN apk add make

COPY Makefile ./
COPY src ./src
RUN make build

ENV PORT 8080
EXPOSE $PORT

CMD [ "java", "-cp", ".:bin:libs/*", "Manager" ]