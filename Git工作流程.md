# Git 工作流程指南

## 🌳 分支策略

### 主分支
- `main` - 生产环境代码，稳定版本
- `develop` - 开发主分支，所有功能合并到这里

### 功能分支
- `feature/模块名-功能` - 功能开发分支
- `fix/模块名-问题` - Bug修复分支
- `refactor/模块名` - 重构分支

## 📝 常用命令

### 1. 初始化和克隆

```bash
# 克隆项目
git clone <项目地址>
cd 软件创新大赛

# 查看分支
git branch -a

# 切换到开发分支
git checkout develop
```

### 2. 创建功能分支

```bash
# 从develop创建新分支
git checkout develop
git pull origin develop
git checkout -b feature/scan-ocr

# 或者一步完成
git checkout -b feature/scan-ocr develop
```

### 3. 日常开发

```bash
# 查看状态
git status

# 添加文件
git add app/src/main/java/com/edusmart/app/feature/scan/ScanScreen.kt

# 提交
git commit -m "feat(scan): 添加AI答题功能"

# 推送
git push origin feature/scan-ocr
```

### 4. 同步最新代码

```bash
# 切换到develop
git checkout develop
git pull origin develop

# 切换回功能分支
git checkout feature/scan-ocr

# 合并develop的最新代码
git merge develop

# 如果有冲突，解决后
git add .
git commit -m "merge: 合并develop最新代码"
```

### 5. 创建Pull Request

1. 推送功能分支到远程
2. 在Git平台（GitHub/GitLab）创建PR
3. 选择从 `feature/xxx` 合并到 `develop`
4. 填写PR描述
5. 等待代码审查

### 6. 代码审查和合并

```bash
# 审查通过后，合并到develop
git checkout develop
git pull origin develop
git merge feature/scan-ocr
git push origin develop

# 删除已合并的功能分支
git branch -d feature/scan-ocr
git push origin --delete feature/scan-ocr
```

## 🔀 解决冲突

### 当合并时出现冲突

```bash
# 1. 查看冲突文件
git status

# 2. 打开冲突文件，解决冲突
# 冲突标记：
# <<<<<<< HEAD
# 你的代码
# =======
# 其他人的代码
# >>>>>>> feature/other-branch

# 3. 解决后
git add .
git commit -m "merge: 解决冲突"
```

## 📋 提交信息规范

### 格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 类型 (type)
- `feat`: 新功能
- `fix`: 修复Bug
- `docs`: 文档更新
- `style`: 代码格式
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建/工具

### 示例

```bash
# 新功能
git commit -m "feat(scan): 添加AI答题功能"

# 修复Bug
git commit -m "fix(scan): 修复OCR识别失败问题"

# 文档更新
git commit -m "docs: 更新API配置指南"

# 重构
git commit -m "refactor(ui): 重构美化组件"
```

## 🚫 避免的操作

1. **不要直接提交到 main**
   - 所有代码先合并到 develop
   - 测试通过后再合并到 main

2. **不要强制推送**
   ```bash
   # ❌ 不要这样做
   git push --force
   
   # ✅ 应该这样做
   git pull
   git push
   ```

3. **不要提交大文件**
   - 使用 .gitignore 排除
   - 大文件使用 Git LFS

4. **不要提交敏感信息**
   - API密钥
   - 密码
   - 个人信息

## 📁 .gitignore 检查

确保以下文件已忽略：
```
*.iml
.gradle/
build/
.idea/
*.apk
*.ap_
*.aab
local.properties
```

---

**按照这个流程，可以高效地协作开发！**

