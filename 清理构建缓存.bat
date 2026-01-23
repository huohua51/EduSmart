@echo off
echo ========================================
echo 清理 Gradle 构建缓存
echo ========================================
echo.

echo [1/4] 清理项目构建文件...
if exist "build" (
    rmdir /s /q "build"
    echo   已删除 build 目录
)
if exist "app\build" (
    rmdir /s /q "app\build"
    echo   已删除 app\build 目录
)

echo [2/4] 清理 Gradle 缓存...
if exist "%USERPROFILE%\.gradle\caches" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches"
    echo   已清理 Gradle 缓存
)

echo [3/4] 清理 .gradle 目录...
if exist ".gradle" (
    rmdir /s /q ".gradle"
    echo   已删除 .gradle 目录
)

echo [4/4] 完成！
echo.
echo 现在可以在 Android Studio 中：
echo 1. File -^> Invalidate Caches / Restart
echo 2. File -^> Sync Project with Gradle Files
echo 3. Build -^> Rebuild Project
echo.
pause

