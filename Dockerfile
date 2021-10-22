FROM openjdk:12 AS builder

RUN mkdir -p /root/src/keva
WORKDIR /root/src/keva

COPY gradle ./gradle
COPY build.gradle gradlew settings.gradle ./
COPY ./server/build.gradle ./server/keva.properties ./server/
RUN ./gradlew dependencies

COPY . .
RUN ./gradlew clean :server:build -x test

FROM openjdk:12-jdk-alpine

RUN mkdir -p /root/binary/keva
WORKDIR /root/binary/keva

COPY --from=builder /root/src/keva/server/build/libs/server-1.0-SNAPSHOT-all.jar /root/binary/keva/keva.jar

EXPOSE 6767

ENTRYPOINT ["java","-jar","keva.jar","--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]

# docker build -t keva-server .
# docker run -d --name keva-server --network=host -p 6767:6767 keva-server:latest
