FROM maven:3.6.3-jdk-8 AS maven
USER root
COPY ./ /tmp/code
RUN cd /tmp/code && mvn -s /tmp/code/settings.xml clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true


FROM java:8
COPY --from=maven /tmp/code/target/*.jar /webdav-teambition.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/webdav-teambition.jar"]
