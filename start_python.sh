#!/bin/bash

# 停止旧进程
PID=$(ps -ef | grep image_parser_service.py | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "Stopping existing Python process: $PID"
    kill -9 $PID
fi

# 进入脚本目录
cd Python_script

# 启动新进程
echo "Starting Python Service..."
# 后台运行，日志写入 python.log
nohup python3 image_parser_service.py > ../python.log 2>&1 &

echo "Python Service started. Check python.log for details."