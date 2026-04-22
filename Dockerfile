FROM maven:3-eclipse-temurin-26-alpine AS build

COPY pom.xml ./
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true



FROM alpine:3.23
ARG VERSION
ENV VERSION=$VERSION
WORKDIR /web/lucos/lucos_media_manager

RUN apk add openjdk25
COPY --from=build target/manager-latest.jar manager.jar

CMD [ "java", "-cp", "manager.jar", "Manager"]