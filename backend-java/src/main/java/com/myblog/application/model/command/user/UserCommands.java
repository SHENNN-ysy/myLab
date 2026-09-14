package com.myblog.application.model.command.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员用户相关的写操作命令集合。
 * <p>
 * 以嵌套 record 形式定义，由 controller 层构造后传入应用服务执行。
 * 长度约束与登录/改密接口（AuthDtos）保持一致，服务层另有兜底校验。
 */
public final class UserCommands {

    private UserCommands() {
    }

    /** 用户名长度下限。 */
    public static final int MIN_USERNAME_LENGTH = 3;
    /** 用户名长度上限。 */
    public static final int MAX_USERNAME_LENGTH = 64;
    /** 密码长度下限。 */
    public static final int MIN_PASSWORD_LENGTH = 8;
    /** 密码长度上限：与 BCrypt 72 字节输入上限保持安全距离，避免静默截断。 */
    public static final int MAX_PASSWORD_LENGTH = 64;

    /** 创建管理员账号命令。 */
    public record Create(
            @NotBlank @Size(min = MIN_USERNAME_LENGTH, max = MAX_USERNAME_LENGTH)
            String username,
            String role,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH)
            String password) {
    }

    /**
     * 更新管理员账号命令。
     * <p>
     * 字段为 null 表示不修改该项，属于部分更新语义。
     */
    public record Update(
            String role,
            Boolean isActive,
            @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH)
            String password) {
    }
}
