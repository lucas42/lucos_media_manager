FROM maven:3.9.5 as build

COPY pom.xml ./
COPY src ./src
RUN mvn clean package



FROM openjdk:18-alpine
WORKDIR /web/lucos/lucos_media_manager
COPY --from=build target/manager-latest.jar manager.jar

ENV PORT 8001
EXPOSE $PORT

CMD [ "java", "-cp", "manager.jar", "Manager"]