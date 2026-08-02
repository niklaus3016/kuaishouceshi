FROM node:20-alpine

WORKDIR /app

# 复制 package.json
COPY package*.json ./

# 安装 serve（静态文件服务器）
RUN npm install serve

# 复制构建好的 dist 目录
COPY dist ./dist

# 复制 entrypoint 脚本
COPY entrypoint.sh /home/devbox/project/entrypoint.sh
RUN chmod +x /home/devbox/project/entrypoint.sh

# 暴露端口
EXPOSE 3003

# 使用 entrypoint 脚本启动
ENTRYPOINT ["/home/devbox/project/entrypoint.sh"]
