FROM java:8
VOLUME /tmp
ADD webdav-teambition.jar /webdav-teambition.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/webdav-teambition.jar"]