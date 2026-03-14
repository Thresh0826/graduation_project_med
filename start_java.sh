#!/bin/bash

# 停止旧进程
PID=$(ps -ef | grep Graduation_Project | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "Stopping existing Java process: $PID"
    kill -9 $PID
fi

# 启动新进程
echo "Starting Java Backend..."
# 使用 prod 配置文件，后台运行，日志写入 java.log
nohup java -jar -Dspring.profiles.active=prod target/Graduation_Project-0.0.1-SNAPSHOT.jar > java.log 2>&1 &

echo "Java Backend started. Check java.log for details."