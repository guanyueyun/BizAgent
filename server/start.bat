@echo off
echo ==================== AI企业低代码开发平台 ====================
echo.

cd /d %~dp0

echo [1/3] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Java环境，请先安装JDK 17+
    pause
    exit /b 1
)

echo [2/3] 检查Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Maven，请先安装Maven
    echo 下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo [3/3] 启动Spring Boot应用...
echo.
mvn spring-boot:run

pause
