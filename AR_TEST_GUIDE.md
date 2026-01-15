# AR功能测试指南

## ✅ 已完成的配置

1. **创建了assets/models目录** ✅
2. **更新了ARActivity.kt** ✅
   - 支持从assets加载模型
   - 支持多个模型文件自动检测
   - 添加了错误处理和提示

## 🚀 测试步骤

### 方法1: 使用本地3D模型（推荐）

#### 步骤1: 下载测试模型

**推荐网站**：
1. **Sketchfab** - https://sketchfab.com/
   - 搜索 "cube" 或 "sphere"
   - 筛选免费模型
   - 下载GLTF格式

2. **快速测试模型推荐**：
   - 搜索 "simple cube" - 简单的立方体
   - 搜索 "sphere" - 球体
   - 搜索 "geometry" - 几何图形

#### 步骤2: 放置模型文件

将下载的模型文件放到：
```
app/src/main/assets/models/geometry.gltf
```

或者重命名为以下任一文件名：
- `geometry.gltf`
- `geometry.glb`
- `molecule.gltf`
- `physics.gltf`

#### 步骤3: 运行应用

1. 同步Gradle依赖
2. 在真机上运行应用（模拟器不支持ARCore）
3. 进入"AR知识空间"
4. 点击"启动AR相机"
5. 将手机对准平面（桌面、地面等）
6. 等待模型出现

### 方法2: 使用在线测试模型（需要网络）

如果需要快速测试，可以使用在线模型：

1. 在 `ARActivity.kt` 的 `loadModelFromNetwork()` 方法中添加模型URL
2. 确保设备连接网络
3. 运行应用测试

### 方法3: 仅测试平面检测（无模型）

即使没有3D模型，也可以测试AR功能：

1. 运行应用
2. 进入AR界面
3. 将手机对准平面
4. 应该能看到平面检测的网格（如果SceneView支持）

## 📱 设备要求

### 必需条件
- ✅ 支持ARCore的Android设备
- ✅ Android 7.0 (API 24) 或更高版本
- ✅ 真机测试（模拟器不支持）

### 检查设备是否支持ARCore

1. 在Google Play搜索"ARCore"
2. 查看是否显示"安装"或"您的设备与此版本不兼容"
3. 或者运行应用，查看是否显示"设备不支持AR"提示

### 支持ARCore的设备列表
- 大部分现代Android手机都支持
- 查看完整列表：https://developers.google.com/ar/devices

## 🐛 常见问题

### 问题1: 显示"设备不支持AR"
**解决**：
- 确认设备支持ARCore
- 检查是否安装了ARCore服务
- 尝试在Google Play安装ARCore应用

### 问题2: 模型不显示
**解决**：
- 检查模型文件是否在 `assets/models/` 目录
- 确认文件名正确（区分大小写）
- 检查模型格式是否为GLTF或GLB
- 查看Logcat中的错误信息

### 问题3: 应用崩溃
**解决**：
- 检查是否在真机上运行（模拟器不支持）
- 查看Logcat中的崩溃日志
- 确认ARCore依赖已正确添加

### 问题4: 平面检测不工作
**解决**：
- 确保光线充足
- 将手机对准有明显纹理的平面（如木桌、地毯）
- 避免纯色平面（如白墙）

## 📊 测试 checklist

- [ ] 设备支持ARCore
- [ ] 在真机上运行应用
- [ ] 进入AR知识空间界面
- [ ] 点击"启动AR相机"
- [ ] 看到相机预览
- [ ] 将手机对准平面
- [ ] 看到平面检测网格（如果有）
- [ ] 3D模型出现（如果有模型文件）
- [ ] 可以旋转模型（手势交互）
- [ ] 可以缩放模型（双指捏合）

## 🎯 下一步

1. **下载测试模型**
   - 从Sketchfab下载一个简单的立方体模型
   - 放到 `assets/models/` 目录

2. **测试基础功能**
   - 验证AR场景可以启动
   - 验证平面检测工作
   - 验证模型可以加载

3. **优化体验**
   - 调整模型大小和位置
   - 添加更多模型
   - 实现模型切换功能

## 📚 参考资源

1. **SceneView文档**: https://github.com/SceneView/sceneview-android
2. **ARCore文档**: https://developers.google.com/ar/develop
3. **GLTF格式**: https://www.khronos.org/gltf/
4. **Sketchfab**: https://sketchfab.com/

---

**提示**: 如果暂时没有3D模型，可以先测试AR场景是否能正常启动和检测平面。模型可以后续添加。

