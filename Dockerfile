ARG JRE_IMAGE_URI=adoptopenjdk/openjdk15:alpine
FROM $JRE_IMAGE_URI
MAINTAINER Sachin Goyal <sachin.goyal.se@gmail.com>
VOLUME /opt
ARG TARGETPLATFORM
ARG SERVICE=service

ADD build/libs/$SERVICE.jar /opt/service/service.jar

ENTRYPOINT ["java","-jar","/opt/service/service.jar"]
