@echo off
REM ====== 在这里填你的配置 ======
set DB_USERNAME=root
set DB_PASSWORD=123456
set AI_API_KEY=sk-b05091000db9402a949b868829ee1d01
REM =============================

echo 启动 Java 后端...
call mvnw spring-boot:run
pause
