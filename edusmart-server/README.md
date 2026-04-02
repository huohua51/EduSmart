# EduSmart Server

Java 后端（Spring Boot + MySQL + JWT），用于替代/承接客户端原先的 CloudBase 调用。

## 本地启动

### 1) 启动 MySQL（可选）

```bash
docker compose up -d
```

默认会创建数据库 `edusmart`，root 密码 `root`。

### 2) 启动后端

Windows PowerShell（建议使用 JDK 17）：

```powershell
cd "edusmart-server"
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat bootRun
```

### 3) 健康检查与文档

- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 现有接口

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET/POST/PUT/DELETE /api/notes`

