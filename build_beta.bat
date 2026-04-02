@echo off
chcp 65001 >nul
echo ========================================
echo 构建 Beta 版本 APK
echo ========================================
echo.

REM 清理之前的构建
echo [1/4] 清理之前的构建...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo 清理失败！
    pause
    exit /b 1
)

echo.
echo [2/4] 构建 Beta 版本...
call gradlew.bat assembleBeta
if %errorlevel% neq 0 (
    echo 构建失败！
    pause
    exit /b 1
)

echo.
echo [3/4] 查找生成的 APK 文件...
set APK_PATH=app\build\outputs\apk\beta\app-beta-beta.apk
if exist "%APK_PATH%" (
    echo ✅ APK 构建成功！
    echo.
    echo APK 位置: %APK_PATH%
    echo.
    
    REM 获取文件大小
    for %%A in ("%APK_PATH%") do set SIZE=%%~zA
    set /a SIZE_MB=%SIZE% / 1024 / 1024
    echo 文件大小: %SIZE_MB% MB
    echo.
    
    echo [4/4] 打开输出目录...
    start explorer "app\build\outputs\apk\beta"
) else (
    echo ❌ 未找到 APK 文件！
    echo 请检查构建日志中的错误信息。
)

echo.
echo ========================================
echo 构建完成！
echo ========================================
pause

