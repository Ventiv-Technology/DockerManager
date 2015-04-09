FROM java:7-jdk
MAINTAINER John Crygier <john.crygier@ventivtech.com>

ADD . /opt/DockerManagerBuild
ADD sample-config /opt/DockerManager/sample-config

WORKDIR /opt/DockerManagerBuild

RUN ./gradlew clean build -x test &&\
    mkdir -p /opt/DockerManager &&\
    cp /opt/DockerManagerBuild/build/libs/dockermanager-*.jar /opt/DockerManager/dockermanager.jar &&\
    rm -rf /root/.gradle/caches/modules* &&\
    rm -rf /opt/DockerManagerBuild

WORKDIR /opt/DockerManager

EXPOSE 8080
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "dockermanager.jar"]