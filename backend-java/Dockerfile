# ============================================================
# 后端镜像：Maven 构建 + JRE 运行（两阶段）
# 基础镜像可通过 build-arg 覆盖，便于从本地镜像仓库拉取：
#   docker build --build-arg MAVEN_IMAGE=localhost:5000/maven:3.9-eclipse-temurin-21 ...
# ============================================================
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre

# ---------- 阶段 1：编译打包 ----------
FROM ${MAVEN_IMAGE} AS builder
WORKDIR /app

# 先只复制 pom.xml 与 Maven 仓库镜像配置（docker/maven-settings.xml，走国内镜像加速），
# 充分利用 Docker 层缓存：依赖不变时跳过重新下载
COPY pom.xml ./
COPY docker/maven-settings.xml /root/.m2/settings.xml
RUN mvn -B -q -s /root/.m2/settings.xml dependency:go-offline

# 再复制源码与质量检查配置（checkstyle/spotbugs 规则），源码变化只重建这一层
COPY src ./src
COPY config ./config
# 构建镜像时执行全部单元测试（纯单测，不依赖 DB/Redis），测试失败则构建失败、无法部署；
# *IT 集成测试（Testcontainers）不在镜像构建期执行，由 CI 流水线负责
RUN mvn -B -q -s /root/.m2/settings.xml package

# ---------- 阶段 2：精简运行镜像 ----------
FROM ${JRE_IMAGE}
WORKDIR /app

# 非 root 用户运行，降低容器逃逸风险
RUN groupadd --system myblog && useradd --system --gid myblog myblog
COPY --from=builder /app/target/myblog-backend-1.0.0.jar app.jar
USER myblog

EXPOSE 8000
# JVM 内存/GC 参数按 2核4G 服务器调优（compose 另设 1g 容器内存上限），OOM 时直接退出便于重启
ENV JAVA_TOOL_OPTIONS="-Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java","-jar","/app/app.jar"]
