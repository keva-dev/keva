FROM openjdk:11 AS builder
RUN mkdir -p /root/src/keva
WORKDIR /root/src/keva

COPY gradle ./gradle
COPY build.gradle gradlew settings.gradle ./
COPY ./app/build.gradle ./app/keva.properties ./app/
RUN ./gradlew dependencies

COPY . .
RUN ./gradlew :app:build -x test


FROM eclipse-temurin:11.0.13_8-jdk-centos7
RUN mkdir -p /root/binary/keva
WORKDIR /root/binary/keva

COPY --from=builder /root/src/keva/app/build/libs/app-1.0.0-rc0-all.jar /root/binary/keva/keva.jar

EXPOSE 6379

ENTRYPOINT ["java","-jar","-Xms24m","-Xmx256m","keva.jar"]

