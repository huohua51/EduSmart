# 通过 USB 在 Android 手机上安装应用

## 📱 准备工作

### 1. 启用开发者选项

1. **打开手机设置**
2. **关于手机** → 找到 **版本号** 或 **内部版本号**
3. **连续点击 7 次**版本号
4. 会提示"您已成为开发者"

### 2. 启用 USB 调试

1. 返回设置主界面
2. 找到 **系统** → **开发者选项**（或 **更多设置** → **开发者选项**）
3. 打开 **USB 调试** 开关
4. （可选）打开 **USB 安装**（如果提示需要）

### 3. 连接手机到电脑

1. 用 USB 线连接手机和电脑
2. 手机上会弹出提示，选择 **传输文件** 或 **文件传输**（MTP 模式）
3. 首次连接时，手机会提示"允许 USB 调试吗？"
   - 勾选 **始终允许来自这台计算机**
   - 点击 **确定**

## 🚀 在 Android Studio 中安装

### 方法1: 直接运行（推荐）

1. **连接手机**（确保 USB 调试已启用）

2. **检查设备连接**：
   - 在 Android Studio 顶部工具栏，点击设备选择下拉菜单
   - 应该能看到您的手机型号
   - 如果看不到，点击 **Refresh** 刷新

3. **选择设备**：
   - 在下拉菜单中选择您的手机

4. **运行应用**：
   - 点击 **▶️ Run** 按钮
   - 或按快捷键：`Shift + F10` (Windows) / `Ctrl + R` (Mac)
   - Android Studio 会自动：
     - 编译代码
     - 打包 APK
     - 安装到手机
     - 启动应用

### 方法2: 使用命令行安装

#### Windows (PowerShell):

```powershell
# 1. 检查设备是否连接
adb devices

# 2. 如果看到设备（显示 device），说明连接成功
# 如果显示 unauthorized，需要在手机上允许 USB 调试

# 3. 安装 APK（需要先构建）
.\gradlew installDebug

# 或者直接安装已构建的 APK
adb install app\build\outputs\apk\debug\app-debug.apk
```

#### Mac/Linux:

```bash
# 1. 检查设备
adb devices

# 2. 安装
./gradlew installDebug

# 或
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🔍 验证连接

### 检查 ADB 连接

在终端或命令提示符中执行：

```powershell
adb devices
```

**正常输出应该类似：**
```
List of devices attached
ABC123XYZ    device
```

**如果显示 `unauthorized`：**
- 在手机上查看是否有"允许 USB 调试"的提示
- 点击 **允许**

**如果显示 `offline`：**
- 拔掉 USB 线，重新连接
- 重新启用 USB 调试

**如果没有任何设备：**
- 检查 USB 线是否支持数据传输（有些线只能充电）
- 检查 USB 驱动是否安装（Windows 可能需要安装手机驱动）
- 尝试不同的 USB 端口

## 📦 手动安装 APK（如果自动安装失败）

### 步骤1: 构建 APK

在 Android Studio 中：
- **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
- 等待构建完成

### 步骤2: 找到 APK 文件

APK 位置：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 步骤3: 传输到手机

**方法A: 通过 USB 传输**
1. 将 APK 文件复制到手机存储
2. 在手机上打开文件管理器
3. 找到 APK 文件，点击安装

**方法B: 使用 ADB 安装**
```powershell
adb install app\build\outputs\apk\debug\app-debug.apk
```

**方法C: 通过微信/QQ 传输**
1. 将 APK 发送到微信/QQ
2. 在手机上接收文件
3. 点击安装

## ⚠️ 常见问题

### 问题1: "adb: command not found"

**解决**：
- Android Studio 会自动配置 ADB
- 或者手动添加到系统 PATH：
  - Windows: `C:\Users\您的用户名\AppData\Local\Android\Sdk\platform-tools`
  - Mac: `~/Library/Android/sdk/platform-tools`

### 问题2: 手机不显示在设备列表中

**解决**：
1. 检查 USB 调试是否启用
2. 尝试不同的 USB 端口
3. 尝试不同的 USB 线
4. 在手机上撤销 USB 调试授权，重新连接
5. 重启 ADB：
   ```powershell
   adb kill-server
   adb start-server
   adb devices
   ```

### 问题3: 安装失败 "INSTALL_FAILED"

**解决**：
```powershell
# 卸载旧版本
adb uninstall com.edusmart.app

# 重新安装
adb install app\build\outputs\apk\debug\app-debug.apk
```

### 问题4: "设备离线" 或 "unauthorized"

**解决**：
1. 在手机上撤销 USB 调试授权
2. 拔掉 USB 线
3. 重新连接
4. 在手机上允许 USB 调试
5. 勾选"始终允许"

### 问题5: Windows 无法识别设备

**解决**：
1. 安装手机厂商的 USB 驱动（如小米、华为、OPPO 等）
2. 或使用通用 Android USB 驱动
3. 在设备管理器中检查是否有未识别的设备

## 🎯 快速检查清单

- [ ] 已启用开发者选项
- [ ] 已启用 USB 调试
- [ ] USB 线已连接
- [ ] 手机上允许了 USB 调试
- [ ] `adb devices` 显示设备为 `device` 状态
- [ ] Android Studio 中能看到设备
- [ ] 已选择正确的设备
- [ ] 点击运行按钮

## 💡 小贴士

1. **保持连接**：安装和调试时保持 USB 连接
2. **使用原装线**：原装 USB 线通常更稳定
3. **关闭省电模式**：某些手机的省电模式可能影响 USB 调试
4. **允许安装未知来源**：如果提示，需要在设置中允许安装未知来源的应用

## 🚀 现在就可以

1. **连接手机**（启用 USB 调试）
2. **在 Android Studio 中选择设备**
3. **点击运行按钮 ▶️**
4. **应用会自动安装并启动**

---

**详细步骤已准备好，现在就可以连接手机并安装应用了！**

