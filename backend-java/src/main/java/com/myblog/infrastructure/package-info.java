/**
 * 基础设施层：实现数据库、缓存、鉴权与对象存储等应用端口。
 * <p>
 * 子包划分：diagnostics 为健康检查探测，persistence 为 MyBatis/JDBC 持久化
 * （mapper、type handler、仓储实现），security 为 JWT 签发与认证过滤，
 * storage 为阿里云 OSS 对象存储适配。
 */
package com.myblog.infrastructure;
