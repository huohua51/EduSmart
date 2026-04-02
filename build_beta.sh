#!/bin/bash

echo "========================================"
echo "构建 Beta 版本 APK"
echo "========================================"
echo ""

# 清理之前的构建
echo "[1/4] 清理之前的构建..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "清理失败！"
    exit 1
fi

echo ""
echo "[2/4] 构建 Beta 版本..."
./gradlew assembleBeta
if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

echo ""
echo "[3/4] 查找生成的 APK 文件..."
APK_PATH="app/build/outputs/apk/beta/app-beta-beta.apk"
if [ -f "$APK_PATH" ]; then
    echo "✅ APK 构建成功！"
    echo ""
    echo "APK 位置: $APK_PATH"
    echo ""
    
    # 获取文件大小
    SIZE=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
    SIZE_MB=$((SIZE / 1024 / 1024))
    echo "文件大小: ${SIZE_MB} MB"
    echo ""
    
    echo "[4/4] 打开输出目录..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open "app/build/outputs/apk/beta"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open "app/build/outputs/apk/beta"
    fi
else
    echo "❌ 未找到 APK 文件！"
    echo "请检查构建日志中的错误信息。"
fi

echo ""
echo "========================================"
echo "构建完成！"
echo "========================================"

