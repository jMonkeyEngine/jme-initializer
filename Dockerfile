FROM azul/zulu-openjdk:17-latest

# this is so that app itself can run as something with very few permissions
RUN groupadd --gid 1000 nonroot
RUN useradd --uid 1000 -r --gid 1000 nonroot

COPY src /app_build/src
COPY gradle /app_build/gradle
COPY build.gradle /app_build
COPY settings.gradle /app_build
COPY gradlew /app_build
COPY package.json /app_build
COPY package-lock.json /app_build
COPY webpack.config.js /app_build

RUN cd /app_build &&\
    chmod +x gradlew&&\
    ./gradlew test bootjar

RUN  chown 1000:1000 /app_build/build/libs/
USER nonroot
WORKDIR /app_build/build/libs
ENTRYPOINT [ "java","-jar","-Dspring.profiles.active=prod","jmeinitializer.jar" ]
