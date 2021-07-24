FROM maven:3-jdk-8-alpine AS maven
USER root
COPY ./ /tmp/code
RUN cd /tmp/code && mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true


FROM openjdk:8-jdk-alpine
COPY --from=maven /tmp/code/target/*.jar /webdav.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx1g"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /webdav.jar"]
