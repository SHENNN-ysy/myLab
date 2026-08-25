# CI 已完成编译和全部测试，本镜像只封装已验证的 JAR。
FROM eclipse-temurin:21-jre

ARG OCI_VERSION
ARG OCI_REVISION
ARG OCI_CREATED
LABEL org.opencontainers.image.title="myblog-api" \
      org.opencontainers.image.version="$OCI_VERSION" \
      org.opencontainers.image.revision="$OCI_REVISION" \
      org.opencontainers.image.created="$OCI_CREATED"

WORKDIR /app
RUN groupadd --system myblog && useradd --system --gid myblog myblog
COPY app.jar /app/app.jar
USER myblog

EXPOSE 8000
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
