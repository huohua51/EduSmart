# 上传项目到 GitHub 并创建模块分支指南

## 第一步：在 GitHub 上创建仓库

1. 访问 https://github.com/huohua51
2. 点击右上角的 "+" 按钮，选择 "New repository"
3. 填写仓库信息：
   - Repository name: `EduSmart` (或你喜欢的名字)
   - Description: `智能教育助手 - Android AR学习应用`
   - 选择 Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"（因为我们已经有了代码）
4. 点击 "Create repository"

## 第二步：在本地准备代码

### 2.1 检查 Git 状态
```powershell
git status
```

### 2.2 添加所有项目文件（排除 .gradle 缓存）
```powershell
git add .
```

### 2.3 创建初始提交
```powershell
git commit -m "feat: 初始提交 - EduSmart 智能教育助手项目"
```

## 第三步：连接到 GitHub 远程仓库

### 3.1 添加远程仓库
```powershell
git remote add origin https://github.com/huohua51/EduSmart.git
```

**注意**：将 `EduSmart` 替换为你实际创建的仓库名称。

### 3.2 验证远程仓库
```powershell
git remote -v
```

## 第四步：推送代码到 GitHub

### 4.1 推送主分支
```powershell
git push -u origin master
```

如果遇到认证问题，你可能需要：
- 使用 Personal Access Token 替代密码
- 或者配置 SSH 密钥

## 第五步：创建开发分支和模块分支

### 5.1 创建并切换到 develop 分支
```powershell
git checkout -b develop
git push -u origin develop
```

### 5.2 创建各个模块分支

#### 模块1：拍照识题模块
```powershell
git checkout -b feature/scan-module develop
git push -u origin feature/scan-module
```

#### 模块2：AR知识空间
```powershell
git checkout -b feature/ar-module develop
git push -u origin feature/ar-module
```

#### 模块3：智能笔记
```powershell
git checkout -b feature/note-module develop
git push -u origin feature/note-module
```

#### 模块4：AI口语私教
```powershell
git checkout -b feature/speaking-module develop
git push -u origin feature/speaking-module
```

#### 模块5：知识雷达
```powershell
git checkout -b feature/radar-module develop
git push -u origin feature/radar-module
```

#### 模块6：UI/UX组件
```powershell
git checkout -b feature/ui-components develop
git push -u origin feature/ui-components
```

#### 模块7：数据层
```powershell
git checkout -b feature/data-layer develop
git push -u origin feature/data-layer
```

## 第六步：设置默认分支为 develop（可选）

1. 在 GitHub 仓库页面，点击 "Settings"
2. 在左侧菜单选择 "Branches"
3. 在 "Default branch" 部分，选择 "develop"
4. 点击 "Update"

## 快速命令脚本

如果你想一次性执行所有操作，可以使用以下 PowerShell 脚本：

```powershell
# 设置变量（请修改为你的仓库名）
$REPO_NAME = "EduSmart"
$GITHUB_USER = "huohua51"

# 1. 添加并提交代码
git add .
git commit -m "feat: 初始提交 - EduSmart 智能教育助手项目"

# 2. 添加远程仓库
git remote add origin https://github.com/$GITHUB_USER/$REPO_NAME.git

# 3. 推送主分支
git push -u origin master

# 4. 创建 develop 分支
git checkout -b develop
git push -u origin develop

# 5. 创建模块分支
$modules = @(
    "feature/scan-module",
    "feature/ar-module", 
    "feature/note-module",
    "feature/speaking-module",
    "feature/radar-module",
    "feature/ui-components",
    "feature/data-layer"
)

foreach ($module in $modules) {
    git checkout -b $module develop
    git push -u origin $module
    Write-Host "已创建分支: $module"
}

# 6. 切换回 develop 分支
git checkout develop
Write-Host "所有分支已创建完成！"
```

## 常见问题

### Q: 如果远程仓库已存在怎么办？
A: 如果远程仓库已经存在，使用以下命令强制推送（谨慎使用）：
```powershell
git push -u origin master --force
```

### Q: 如何配置 GitHub 认证？
A: 推荐使用 Personal Access Token：
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 生成新 token，勾选 `repo` 权限
3. 使用 token 作为密码

### Q: 如何查看所有分支？
A: 
```powershell
git branch -a
```

### Q: 如何切换到某个模块分支？
A:
```powershell
git checkout feature/scan-module
```

## 下一步

完成上传后，团队成员可以：
1. 克隆仓库：`git clone https://github.com/huohua51/EduSmart.git`
2. 切换到自己的模块分支：`git checkout feature/你的模块名`
3. 开始开发

详细的工作流程请参考：
- `分工开发流程指南.md`
- `Git工作流程.md`
- `模块开发任务清单.md`

