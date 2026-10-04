FROM openjdk:27-ea-oraclelinux9

ARG JAR_PACKAGE=target/*.jar

COPY ${JAR_PACKAGE} app.jar

CMD ["java", "-jar", "/app.jar"]