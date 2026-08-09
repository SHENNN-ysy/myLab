# 树莓派搭家用 NAS：Samba 与硬盘休眠

## Samba 共享配置

通过 `valid users` 和 `create mask` 控制访问与新建文件权限。挂载点适合交给 systemd 管理，并使用 `nofail` 防止硬盘缺失时阻塞启动。

## 硬盘休眠

机械盘可以通过 `hdparm -S` 设置闲置休眠。部分 USB 硬盘盒不支持指令透传，此时可以使用 `hd-idle` 作为替代方案。
