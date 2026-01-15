# EduSmart Project Upload to GitHub Script
# Usage: Run this script in PowerShell

param(
    [string]$RepoName = "EduSmart",
    [string]$GitHubUser = "huohua51"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EduSmart Project Upload to GitHub" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if in Git repository
if (-not (Test-Path .git)) {
    Write-Host "Error: Current directory is not a Git repository" -ForegroundColor Red
    Write-Host "Please run: git init" -ForegroundColor Yellow
    exit 1
}

# 1. Add all files
Write-Host "[1/6] Adding files to staging area..." -ForegroundColor Yellow
git add .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: git add encountered some issues, but continuing..." -ForegroundColor Yellow
}

# 2. Check for uncommitted changes
$status = git status --porcelain
if ($status) {
    Write-Host "[2/6] Creating initial commit..." -ForegroundColor Yellow
    $commitMessage = @"
feat: Initial commit - EduSmart Education Assistant Project

- Add photo question recognition (OCR + AI answering)
- Add AR knowledge space feature
- Add smart notes feature
- Add AI speaking tutor feature
- Add knowledge radar feature
- Add beautiful UI components
- Configure data layer (Room database)
- Integrate Xunfei Speech SDK
- Integrate Doubao AI API
"@
    git commit -m $commitMessage
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Commit failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] Commit successful" -ForegroundColor Green
} else {
    Write-Host "[2/6] No changes to commit, skipping..." -ForegroundColor Yellow
}

# 3. Check remote repository
Write-Host "[3/6] Checking remote repository configuration..." -ForegroundColor Yellow
$remote = git remote get-url origin 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Adding remote repository..." -ForegroundColor Yellow
    git remote add origin "https://github.com/$GitHubUser/$RepoName.git"
    Write-Host "[OK] Remote repository added" -ForegroundColor Green
} else {
    Write-Host "Remote repository already exists: $remote" -ForegroundColor Green
}

# 4. Push main branch
Write-Host "[4/6] Pushing master branch to GitHub..." -ForegroundColor Yellow
Write-Host "Note: If this is the first push, you may need to enter GitHub username and password/Token" -ForegroundColor Cyan
git push -u origin master
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Push failed" -ForegroundColor Red
    Write-Host "Possible reasons:" -ForegroundColor Yellow
    Write-Host "  1. GitHub repository not created yet, please visit https://github.com/$GitHubUser to create repository" -ForegroundColor Yellow
    Write-Host "  2. Authentication failed, please use Personal Access Token" -ForegroundColor Yellow
    Write-Host "  3. Network issue" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] master branch pushed" -ForegroundColor Green

# 5. Create develop branch
Write-Host "[5/6] Creating develop branch..." -ForegroundColor Yellow
$currentBranch = git branch --show-current
if ($currentBranch -ne "develop") {
    git checkout -b develop 2>$null
    if ($LASTEXITCODE -ne 0) {
        git checkout develop
    }
}
git push -u origin develop
if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: develop branch push failed, but continuing to create module branches..." -ForegroundColor Yellow
} else {
    Write-Host "[OK] develop branch created and pushed" -ForegroundColor Green
}

# 6. Create module branches
Write-Host "[6/6] Creating module branches..." -ForegroundColor Yellow
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
    Write-Host "  Creating branch: $module" -ForegroundColor Cyan
    git checkout -b $module develop 2>$null
    if ($LASTEXITCODE -ne 0) {
        git checkout $module
    }
    git push -u origin $module 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "    [OK] $module created and pushed" -ForegroundColor Green
    } else {
        Write-Host "    [WARN] $module push failed (may already exist)" -ForegroundColor Yellow
    }
}

# Switch back to develop branch
git checkout develop

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Repository URL: https://github.com/$GitHubUser/$RepoName" -ForegroundColor Cyan
Write-Host ""
Write-Host "Created branches:" -ForegroundColor Yellow
Write-Host "  - master (main branch)" -ForegroundColor White
Write-Host "  - develop (development branch)" -ForegroundColor White
foreach ($module in $modules) {
    Write-Host "  - $module" -ForegroundColor White
}
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Team members can clone: git clone https://github.com/$GitHubUser/$RepoName.git" -ForegroundColor White
Write-Host "  2. Switch to your module branch: git checkout feature/your-module-name" -ForegroundColor White
Write-Host "  3. Start developing!" -ForegroundColor White
Write-Host ""
