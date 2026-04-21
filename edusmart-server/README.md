# EduSmart 后端（Spring Boot + Spring AI）

这是 EduSmart Android 应用的后端服务，替代原有的腾讯云开发云函数
`cloudbase-auth` 和 `cloudbase-note`，并把原本在客户端直连的大模型 API
全部收敛到后端、由 **Spring AI** 统一调用。

- Java 17，Spring Boot 3.4，Spring AI 1.0（`spring-ai-starter-model-openai`）
- 默认使用 H2 文件数据库 + 本地文件存储，开箱即用
- Bearer Token 鉴权（所有 `/api/**` 接口除登录/注册/健康检查均需 Token）

## 目录结构

```
edusmart-server/
├── pom.xml
├── src/main/java/com/edusmart/server/
│   ├── EdusmartServerApplication.java
│   ├── common/          # 统一响应 / 异常处理
│   ├── config/          # CORS / 拦截器 / 属性
│   ├── controller/      # Auth / Note / AI / Health
│   ├── dto/             # 请求响应 DTO
│   ├── entity/          # JPA 实体
│   ├── repository/      # Spring Data Repository
│   ├── security/        # Token 拦截器
│   └── service/         # 业务
└── src/main/resources/
    └── application.yml
```

## 启动

```bash
cd edusmart-server

# 大模型 API Key（二选一，推荐前者）
# 阿里云百炼 / MaaS 工作空间网关（OpenAI 兼容）：
#   $env:ALIYUN_MAAS_API_KEY="sk-xxxxxxxx"
# 公网 DashScope（未用网关时）：
#   $env:TONGYI_API_KEY="sk-xxxxxxxx"
# Windows PowerShell 示例：
$env:ALIYUN_MAAS_API_KEY="sk-xxxxxxxx"
# macOS / Linux:
export ALIYUN_MAAS_API_KEY=sk-xxxxxxxx

mvn spring-boot:run
```

`application.yml` 里已配置为 **MaaS 兼容地址**（`.../compatible-mode`）。
Spring AI 会在其后自动拼接 `/v1/chat/completions`，勿在 `base-url` 末尾多写 `/v1`。

若调用模型报错「模型不存在」，请在百炼控制台查看**实际模型名或部署名**，修改：

`spring.ai.openai.chat.options.model`

启动后：
- HTTP 服务：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`
  （JDBC URL：`jdbc:h2:file:./data/edusmart`，用户名 `sa`，密码空）
- 健康检查：`GET /api/health`

## API 约定

所有接口统一响应体：

```json
{ "code": 0, "message": "ok", "data": { ... } }
```

`code == 0` 表示成功；其它表示失败。需要认证的接口在请求头中携带
`Authorization: Bearer <token>`（也支持 `?token=xxx` 作为兜底）。

### Auth（`/api/auth/*`）

| 方法 | 路径 | 鉴权 | Body | 返回 data |
|------|------|------|------|-----------|
| POST | `/register` | 否 | `{email, password, username}` | `UserDto`（含 token） |
| POST | `/login` | 否 | `{email, password}` | `UserDto`（含 token） |
| POST | `/logout` | 是 | — | — |
| GET  | `/me` | 是 | — | `UserDto` |
| PUT  | `/me` | 是 | `{username?, avatarUrl?}` | `UserDto` |
| POST | `/avatar` | 是 | `{imageBase64 或 image, fileName?}` | `{url, cloudPath, fileId}` |

### Note（`/api/notes/*`）

| 方法 | 路径 | 鉴权 | Body/Query | 返回 data |
|------|------|------|------|-----------|
| GET | `/api/notes` | 是 | `?subject=xxx` | `NoteDto[]` |
| GET | `/api/notes/{id}` | 是 | — | `NoteDto` |
| POST | `/api/notes` | 是 | 见下 | `NoteDto` |
| PUT | `/api/notes/{id}` | 是 | 同上（字段可选） | `NoteDto` |
| DELETE | `/api/notes/{id}` | 是 | — | — |
| POST | `/api/notes/files` | 是 | `{fileBase64, fileName, fileType?}` | `{url, cloudPath, fileId}` |

`SaveNoteRequest`：

```json
{
  "id": "可选 uuid",
  "title": "必填",
  "subject": "可选",
  "content": "可选",
  "images": ["..."],
  "audioPath": "可选",
  "transcript": "可选",
  "knowledgePoints": ["..."]
}
```

### AI（`/api/ai/notes/*`，全部需要鉴权）

| 方法 | 路径 | Body | 返回 data |
|------|------|------|-----------|
| POST | `/polish` | `{content, subject?}` | `{text}` |
| POST | `/summary` | `{content, subject?}` | `{summary, keyPoints[], tags[]}` |
| POST | `/title` | `{content, subject?}` | `{title}` |
| POST | `/knowledge-points` | `{content, subject?}` | `{points[]}` |
| POST | `/subject` | `{content, title?}` | `{subject}` |
| POST | `/qa` | `{content, question}` | `{answer}` |

## 配置说明

`application.yml` 主要项：

```yaml
spring.ai.openai.base-url: https://<你的网关>.cn-beijing.maas.aliyuncs.com/compatible-mode
spring.ai.openai.api-key: ${ALIYUN_MAAS_API_KEY:${TONGYI_API_KEY:}}
spring.ai.openai.chat.options.model: qwen-turbo   # 按控制台实际可调用模型修改

edusmart.token-ttl-ms: 2592000000     # Token 过期时间
edusmart.upload-dir: ./uploads        # 文件落盘路径
edusmart.public-file-prefix: /files   # 对外访问前缀
edusmart.public-base-url: ""           # 对外基础 URL，空则返回相对路径
```

> Spring AI 的 OpenAI 兼容 base-url 只需填到根路径；
> 内部会自动拼接 `/v1/chat/completions`。
> DashScope 的对应 base-url 为 `https://dashscope.aliyuncs.com/compatible-mode`。

## 切换到 MySQL（可选）

替换 `application.yml` 中的 `spring.datasource`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/edusmart?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your-password
  jpa:
    hibernate:
      ddl-auto: update
    database-platform: org.hibernate.dialect.MySQLDialect
```

## Android 端对接

客户端 `SDKConfig.SERVER_BASE_URL` 指向后端地址（模拟器访问宿主机用
`http://10.0.2.2:8080`，真机使用服务器 IP）。客户端现有的
`CloudBaseService` / `CloudBaseNoteService` / `NoteAIService` 已改造为调用
本后端对应的 REST 端点，无需修改业务层代码。
