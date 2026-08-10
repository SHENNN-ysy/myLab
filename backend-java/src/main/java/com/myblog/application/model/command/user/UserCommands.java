package com.myblog.application.model.command.user;

/**
 * 管理员用户相关的写操作命令集合。
 * <p>
 * 以嵌套 record 形式定义，由 controller 层构造后传入应用服务执行。
 */
public final class UserCommands {

    private UserCommands() {
    }

    /** 创建管理员账号命令。 */
    public record Create(
            String username,
            String role,
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
            String password) {
    }
}
