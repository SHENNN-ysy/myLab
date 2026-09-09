import sys

path = r'D:\MyLab\article\CI-CD学习实验记录.md'

with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Line 24 (index 23) - fix the duplicate content
lines[23] = '这套方案在项目早期确实很省心：概念少、搭建快，一个 Compose 文件就能完成数据库、缓存、后端、前端和网关的编排。但随着我想给项目加上 HTTPS、让后台走独立路由、尝试引入回滚能力，我慢慢发现"能部署"和"能可控地发布"是两回事。\n'

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)

print('Fixed line 24')
