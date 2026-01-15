@echo off
chcp 65001 >nul
echo 正在复制原生库文件...

REM 创建目标目录
if not exist "app\src\main\jniLibs" mkdir "app\src\main\jniLibs"

REM 复制文件
xcopy /E /I /Y "C:\Users\18335\Desktop\SparkChain_Android_SDK_2.0.1_rc1\SparkChain_Android_SDK_2.0.1_rc1\app\libs\SparkChain_extracted\jni\*" "app\src\main\jniLibs\"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ 原生库文件复制成功！
    echo.
    echo 验证文件：
    if exist "app\src\main\jniLibs\arm64-v8a\libSparkChain.so" (
        echo   ✅ arm64-v8a/libSparkChain.so
    ) else (
        echo   ❌ arm64-v8a/libSparkChain.so 不存在
    )
    
    if exist "app\src\main\jniLibs\arm64-v8a\libspark.so" (
        echo   ✅ arm64-v8a/libspark.so
    ) else (
        echo   ❌ arm64-v8a/libspark.so 不存在
    )
    
    if exist "app\src\main\jniLibs\armeabi-v7a\libSparkChain.so" (
        echo   ✅ armeabi-v7a/libSparkChain.so
    ) else (
        echo   ❌ armeabi-v7a/libSparkChain.so 不存在
    )
    
    if exist "app\src\main\jniLibs\armeabi-v7a\libspark.so" (
        echo   ✅ armeabi-v7a/libspark.so
    ) else (
        echo   ❌ armeabi-v7a/libspark.so 不存在
    )
) else (
    echo.
    echo ❌ 复制失败！请检查 SDK 路径是否正确。
    echo.
    echo 请确认以下路径存在：
    echo C:\Users\18335\Desktop\SparkChain_Android_SDK_2.0.1_rc1\SparkChain_Android_SDK_2.0.1_rc1\app\libs\SparkChain_extracted\jni\
)

echo.
pause



