ARG JENKINS_IMAGE=jenkins/jenkins:lts-jdk21
FROM ${JENKINS_IMAGE}

ARG DOCKER_APT_MIRROR=https://mirrors.ustc.edu.cn/docker-ce/linux/debian

USER root

# 首次更新前替换 Debian 系统源，再通过国内 Docker CE APT 镜像安装工具链。
RUN find /etc/apt -type f \( -name '*.list' -o -name '*.sources' \) \
        -exec sed -Ei \
            -e 's|https?://deb\.debian\.org/debian-security|https://mirrors.ustc.edu.cn/debian-security|g' \
            -e 's|https?://security\.debian\.org/debian-security|https://mirrors.ustc.edu.cn/debian-security|g' \
            -e 's|https?://deb\.debian\.org/debian|https://mirrors.ustc.edu.cn/debian|g' \
            {} + \
    && apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl jq util-linux \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL "${DOCKER_APT_MIRROR}/gpg" \
        -o /etc/apt/keyrings/docker.asc \
    && chmod a+r /etc/apt/keyrings/docker.asc \
    && printf '%s\n' \
        'Types: deb' \
        "URIs: ${DOCKER_APT_MIRROR}" \
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
    && rm -rf /var/lib/apt/lists/*

USER jenkins
