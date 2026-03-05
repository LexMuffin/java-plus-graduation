FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENV EUREKA_URI=http://discovery-server:8761/eureka/
ENV SPRING_CONFIG_IMPORT=configserver:http://config-server:8888
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]