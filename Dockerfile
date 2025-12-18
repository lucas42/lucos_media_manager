FROM maven:3.9.12 AS build

COPY pom.xml ./
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true



FROM alpine:3.23
WORKDIR /web/lucos/lucos_media_manager

RUN apk add openjdk21
COPY --from=build target/manager-latest.jar manager.jar

ENV PORT 8001
EXPOSE $PORT

CMD [ "java", "-cp", "manager.jar", "Manager"]