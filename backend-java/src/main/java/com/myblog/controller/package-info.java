/**
 * 接口层（Web 层）：负责 HTTP 请求解析、参数校验入口、调用 application 层服务并封装统一响应，
 * 不承载业务逻辑；全局异常到统一错误响应的转换由 advice 子包的 GlobalExceptionHandler 完成。
 */
package com.myblog.controller;
