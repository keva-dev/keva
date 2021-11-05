FROM openjdk:11 AS builder

RUN mkdir -p /root/src/keva
WORKDIR /root/src/keva

COPY gradle ./gradle
COPY build.gradle gradlew settings.gradle ./
COPY ./server/build.gradle ./server/keva.properties ./server/
RUN ./gradlew dependencies

COPY . .
RUN ./gradlew :server:build -x test

FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-alpine

RUN mkdir -p /root/binary/keva
WORKDIR /root/binary/keva

COPY --from=builder /root/src/keva/server/build/libs/server-1.0-SNAPSHOT-all.jar /root/binary/keva/keva.jar

EXPOSE 6379

ENTRYPOINT ["java","-jar","keva.jar"]

# docker build -t keva-server .
# docker run -d --name keva-server --network=host -p 6767:6379 keva-server:latest
