ARG JENKINS_IMAGE=jenkins/jenkins:lts-jdk21
FROM ${JENKINS_IMAGE}

ARG REG_VERSION=0.16.1

USER root

# Jenkins 官方镜像不包含 Docker CLI；在节点镜像中一次性安装 CLI、Buildx 与 Compose。
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl util-linux \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
        -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && printf '%s\n' \
        'Types: deb' \
        'URIs: https://download.docker.com/linux/debian' \
        "Suites: $(. /etc/os-release && echo "$VERSION_CODENAME")" \
        'Components: stable' \
        "Architectures: $(dpkg --print-architecture)" \
        'Signed-By: /etc/apt/keyrings/docker.asc' \
        > /etc/apt/sources.list.d/docker.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        docker-ce-cli \
        docker-buildx-plugin \
        docker-compose-plugin \
    && architecture="$(dpkg --print-architecture)" \
    && case "$architecture" in \
        amd64) reg_arch=amd64; reg_sha256=ade837fc5224acd8c34732bf54a94f579b47851cc6a7fd5899a98386b782e228 ;; \
        arm64) reg_arch=arm64; reg_sha256=3dead33ae4e6c75e88fba86b0b6c5ef314c30733ba1d9b0866cd186646fffb7a ;; \
        *) echo "不支持的 reg 架构: $architecture" >&2; exit 1 ;; \
       esac \
    && curl -fsSL "https://github.com/genuinetools/reg/releases/download/v${REG_VERSION}/reg-linux-${reg_arch}" \
        -o /usr/local/bin/reg \
    && echo "${reg_sha256}  /usr/local/bin/reg" | sha256sum -c - \
    && chmod 0755 /usr/local/bin/reg \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
