ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre

FROM ${MAVEN_IMAGE} AS builder
WORKDIR /app
COPY pom.xml ./
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN mvn -B -q -s /root/.m2/settings.xml dependency:go-offline
COPY src ./src
RUN mvn -B -q -s /root/.m2/settings.xml package -DskipTests

FROM ${JRE_IMAGE}
WORKDIR /app
RUN groupadd --system myblog && useradd --system --gid myblog myblog
COPY --from=builder /app/target/myblog-backend-1.0.0.jar app.jar
USER myblog
EXPOSE 8000
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
