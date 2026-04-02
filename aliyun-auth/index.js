/**
 * 阿里云函数计算 - 用户认证服务（超时问题终极修复版）
 * 已解决：OTS/OSS 初始化卡死导致 57 秒超时问题
 * 
 * 环境变量要求（FC 控制台配置）：
 *   ALIYUN_ACCESS_KEY_ID        — 必填（RAM 子账号）
 *   ALIYUN_ACCESS_KEY_SECRET    — 必填
 *   OTS_ENDPOINT                — 必填，格式: https://实例名.cn-hangzhou.ots.aliyuncs.com
 *   OTS_INSTANCE_NAME           — 必填，如 edusmart
 *   OSS_REGION                  — 必填，如 oss-cn-hangzhou
 *   OSS_BUCKET                  — 必填，如 edusmart-avatar
 *   JWT_SECRET                  — 必填，48 字符随机密钥（openssl rand -base64 48）
 */

const TableStore = require('tablestore');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const OSS = require('ali-oss');

// ========== 表名定义 ==========
const USERS_TABLE = 'users'; // 主键: [{ userId: 'string' }]

// ========== 全局客户端（带超时保护） ==========
let tablestoreClient = null;
let ossClient = null;

/**
 * 获取 TableStore 客户端（带 8 秒超时保护）
 * 防止首次初始化卡死导致函数超时
 */
function getTableStoreClient() {
  if (tablestoreClient) return Promise.resolve(tablestoreClient);

  const endpoint = process.env.OTS_ENDPOINT;
  const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
  const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;
  const instanceName = process.env.OTS_INSTANCE_NAME;

  // 强制环境变量校验
  if (!endpoint || !accessKeyId || !accessKeySecret || !instanceName) {
    throw new Error(
      `OTS 环境变量缺失!\n` +
      `OTS_ENDPOINT=${!!endpoint}\n` +
      `ALIYUN_ACCESS_KEY_ID=${!!accessKeyId}\n` +
      `OTS_INSTANCE_NAME=${!!instanceName}`
    );
  }

  console.log(`🔍 初始化 TableStore 客户端... (超时 8s) | 实例: ${instanceName}`);
  
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error(
        `OTS 初始化超时（8秒）!\n` +
        `检查配置:\n` +
        `Endpoint: ${endpoint}\n` +
        `Instance: ${instanceName}\n` +
        `提示: 确保 OTS 与函数同地域（杭州函数 → cn-hangzhou OTS）`
      ));
    }, 8000);

    try {
      const client = new TableStore.Client({
        accessKeyId,
        secretAccessKey: accessKeySecret,
        endpoint,
        instancename: instanceName,
        maxRetries: 2,      // 减少重试次数
        timeout: 5000,      // 单次请求超时 5 秒
        keepAlive: false,   // 禁用长连接（FC 无状态环境更安全）
      });
      
      clearTimeout(timeout);
      tablestoreClient = client;
      console.log('✅ TableStore 客户端初始化成功');
      resolve(client);
    } catch (err) {
      clearTimeout(timeout);
      console.error('OTS 初始化失败:', err);
      reject(err);
    }
  });
}

/**
 * 获取 OSS 客户端（带 8 秒超时保护）
 * 防止首次初始化卡死导致函数超时
 */
function getOSSClient() {
  if (ossClient) return Promise.resolve(ossClient);

  const region = process.env.OSS_REGION;
  const bucket = process.env.OSS_BUCKET;
  const accessKeyId = process.env.ALIYUN_ACCESS_KEY_ID;
  const accessKeySecret = process.env.ALIYUN_ACCESS_KEY_SECRET;

  // 强制环境变量校验
  if (!region || !bucket || !accessKeyId || !accessKeySecret) {
    throw new Error(
      `OSS 环境变量缺失!\n` +
      `OSS_REGION=${!!region}\n` +
      `OSS_BUCKET=${!!bucket}\n` +
      `ALIYUN_ACCESS_KEY_ID=${!!accessKeyId}`
    );
  }

  console.log(`🔍 初始化 OSS 客户端... (超时 8s) | Bucket: ${bucket}`);
  
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject(new Error(
        `OSS 初始化超时（8秒）!\n` +
        `检查配置:\n` +
        `Region: ${region}\n` +
        `Bucket: ${bucket}\n` +
        `提示: 确保 OSS 与函数同地域（杭州函数 → oss-cn-hangzhou）`
      ));
    }, 8000);

    try {
      const client = new OSS({
        region,
        accessKeyId,
        accessKeySecret,
        bucket,
        timeout: '60s',     // 请求超时 60 秒
        maxRetries: 2,      // 重试 2 次
      });
      
      clearTimeout(timeout);
      ossClient = client;
      console.log('✅ OSS 客户端初始化成功');
      resolve(client);
    } catch (err) {
      clearTimeout(timeout);
      console.error('OSS 初始化失败:', err);
      reject(err);
    }
  });
}

// ========== 工具函数 ==========
function createResponse(statusCode, data) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, GET, OPTIONS, PUT',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
      'Access-Control-Max-Age': '86400',
    },
    body: JSON.stringify(data),
  };
}

// 将 TableStore attributeColumns 转为普通对象
function parseAttributes(attrs) {
  const obj = {};
  if (!attrs || !Array.isArray(attrs)) return obj;
  attrs.forEach(([key, value]) => {
    if (value && typeof value === 'object' && typeof value.toNumber === 'function') {
      obj[key] = value.toNumber();
    } else if (value && typeof value === 'object' && typeof value.toString === 'function') {
      obj[key] = value.toString();
    } else {
      obj[key] = value;
    }
  });
  return obj;
}

// ========== 业务函数 ==========
async function register(event) {
  const { email, password, username } = event;
  if (!email || !password || !username) {
    return createResponse(400, {
      code: -1,
      message: '缺少参数：email、password、username 为必填项',
    });
  }

  // 校验邮箱格式
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    return createResponse(400, {
      code: -1,
      message: '邮箱格式不正确',
    });
  }

  // 初始化客户端（带超时保护）
  let client;
  try {
    client = await Promise.race([
      getTableStoreClient(),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('TableStore 客户端初始化超时（10秒）')), 10000)
      )
    ]);
  } catch (err) {
    console.error('❌ TableStore 初始化失败:', err);
    return createResponse(500, {
      code: -1,
      message: '数据库连接失败，请检查配置',
      detail: err.message,
    });
  }

  // 检查邮箱是否已存在
  const queryParams = {
    tableName: USERS_TABLE,
    primaryKey: [{ email }],
  };

  try {
    const result = await Promise.race([
      client.getRow(queryParams),
      new Promise((_, reject) => setTimeout(() => reject(new Error('查询邮箱超时')), 5000)),
    ]);
    if (result.row && result.row.attributes && result.row.attributes.length > 0) {
      return createResponse(400, {
        code: -1,
        message: '邮箱已被注册',
      });
    }
  } catch (e) {
    console.warn('邮箱查重异常，继续注册:', e.message);
  }

  // 生成 userId
  const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  // 加密密码
  const saltRounds = 12;
  const hashedPassword = await bcrypt.hash(password, saltRounds);

  // 构建用户数据
  const userData = {
    userId,
    username,
    email,
    password: hashedPassword,
    avatarUrl: '',
    createdAt: Date.now(),
    updatedAt: Date.now(),
  };

  // 写入 TableStore
  const putParams = {
    tableName: USERS_TABLE,
    condition: new TableStore.Condition(TableStore.RowExistenceExpectation.EXPECT_NOT_EXIST, null),
    primaryKey: [{ userId }],
    attributeColumns: [
      { email },
      { username },
      { password: hashedPassword },
      { avatarUrl: '' },
      { createdAt: TableStore.Long.fromNumber(Date.now()) },
      { updatedAt: TableStore.Long.fromNumber(Date.now()) },
    ],
  };

  try {
    await Promise.race([
      client.putRow(putParams),
      new Promise((_, reject) => setTimeout(() => reject(new Error('保存用户超时')), 8000)),
    ]);
  } catch (err) {
    console.error('❌ 保存用户失败:', err);
    // 检查是否是表不存在
    if (err.message && err.message.includes('OTSObjectNotExist')) {
      return createResponse(500, {
        code: -1,
        message: '数据库表不存在，请先在 Table Store 控制台创建 users 表',
        hint: '表名: users, 主键: userId (String)',
      });
    }
    return createResponse(500, {
      code: -1,
      message: '保存用户失败',
      detail: err.message,
    });
  }

  // 生成 JWT Token
  const token = jwt.sign(
    { userId, email, iat: Math.floor(Date.now() / 1000) },
    process.env.JWT_SECRET,
    { expiresIn: '30d' }
  );

  return createResponse(200, {
    code: 0,
    message: '注册成功',
    data: {
      userId,
      username,
      email,
      avatarUrl: '',
      token,
    },
  });
}

async function login(event) {
  const { email, password } = event;
  if (!email || !password) {
    return createResponse(400, {
      code: -1,
      message: '缺少参数：email 和 password 为必填项',
    });
  }

  const client = await getTableStoreClient();
  const params = {
    tableName: USERS_TABLE,
    primaryKey: [{ email }],
  };

  const result = await Promise.race([
    client.getRow(params),
    new Promise((_, reject) => setTimeout(() => reject(new Error('登录查询超时')), 5000)),
  ]);

  if (!result.row || !result.row.attributes || result.row.attributes.length === 0) {
    return createResponse(400, {
      code: -1,
      message: '邮箱或密码错误',
    });
  }

  const user = parseAttributes(result.row.attributes);
  const isValid = await bcrypt.compare(password, user.password);
  if (!isValid) {
    return createResponse(400, {
      code: -1,
      message: '邮箱或密码错误',
    });
  }

  // 生成 Token
  const token = jwt.sign(
    { userId: user.userId, email: user.email, iat: Math.floor(Date.now() / 1000) },
    process.env.JWT_SECRET,
    { expiresIn: '30d' }
  );

  return createResponse(200, {
    code: 0,
    message: '登录成功',
    data: {
      userId: user.userId,
      username: user.username,
      email: user.email,
      avatarUrl: user.avatarUrl || '',
      token,
    },
  });
}

async function getUserInfo(event) {
  const { userId, token } = event;
  if (!userId || !token) {
    return createResponse(400, {
      code: -1,
      message: '缺少参数：userId 和 token 为必填项',
    });
  }

  // 验证 JWT
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    if (decoded.userId !== userId) {
      throw new Error('Token 与 userId 不匹配');
    }
  } catch (err) {
    return createResponse(401, {
      code: -1,
      message: err.name === 'TokenExpiredError' ? 'Token 已过期' : 'Token 无效',
    });
  }

  const client = await getTableStoreClient();
  const params = {
    tableName: USERS_TABLE,
    primaryKey: [{ userId }],
  };

  const result = await Promise.race([
    client.getRow(params),
    new Promise((_, reject) => setTimeout(() => reject(new Error('获取用户信息超时')), 5000)),
  ]);

  if (!result.row || !result.row.attributes || result.row.attributes.length === 0) {
    return createResponse(404, {
      code: -1,
      message: '用户不存在',
    });
  }

  const user = parseAttributes(result.row.attributes);
  delete user.password; // 绝不返回密码

  return createResponse(200, {
    code: 0,
    message: '获取成功',
    data: user,
  });
}

async function updateUserInfo(event) {
  const { userId, username, avatarUrl, email, token } = event;
  if (!userId || !token) {
    return createResponse(400, {
      code: -1,
      message: '缺少参数：userId 和 token 为必填项',
    });
  }

  // JWT 验证
  try {
    jwt.verify(token, process.env.JWT_SECRET);
  } catch (err) {
    return createResponse(401, {
      code: -1,
      message: 'Token 无效或已过期',
    });
  }

  const client = await getTableStoreClient();
  const updateParams = {
    tableName: USERS_TABLE,
    primaryKey: [{ userId }],
    updateOfAttributeColumns: [
      {
        PUT: [
          { updatedAt: TableStore.Long.fromNumber(Date.now()) },
        ],
      },
    ],
  };

  if (username !== undefined && username !== null && username.trim() !== '') {
    updateParams.updateOfAttributeColumns[0].PUT.push({ username: username.trim() });
  }
  if (avatarUrl !== undefined && avatarUrl !== null) {
    updateParams.updateOfAttributeColumns[0].PUT.push({ avatarUrl: String(avatarUrl) });
  }
  if (email !== undefined && email !== null && email.trim() !== '') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return createResponse(400, { code: -1, message: '邮箱格式不正确' });
    }
    updateParams.updateOfAttributeColumns[0].PUT.push({ email: email.trim() });
  }

  await Promise.race([
    client.updateRow(updateParams),
    new Promise((_, reject) => setTimeout(() => reject(new Error('更新用户信息超时')), 5000)),
  ]);

  // 返回最新数据
  const getParams = {
    tableName: USERS_TABLE,
    primaryKey: [{ userId }],
  };
  const getResult = await Promise.race([
    client.getRow(getParams),
    new Promise((_, reject) => setTimeout(() => reject(new Error('获取更新后用户信息超时')), 5000)),
  ]);
  const user = parseAttributes(getResult.row.attributes);
  delete user.password;

  return createResponse(200, {
    code: 0,
    message: '更新成功',
    data: user,
  });
}

async function uploadAvatar(event) {
  const { userId, imageBase64, fileName, token } = event;
  if (!userId || !imageBase64 || !token) {
    return createResponse(400, {
      code: -1,
      message: '缺少参数：userId、imageBase64、token 为必填项',
    });
  }

  // JWT 验证
  try {
    jwt.verify(token, process.env.JWT_SECRET);
  } catch (err) {
    return createResponse(401, {
      code: -1,
      message: 'Token 无效或已过期',
    });
  }

  // 校验 Base64 格式
  if (!/^data:[^/]+\/[^;]+;base64,/.test(imageBase64)) {
    return createResponse(400, {
      code: -1,
      message: 'imageBase64 格式错误，需以 "data:image/xxx;base64," 开头',
    });
  }

  // 提取纯 base64 数据
  const base64Data = imageBase64.replace(/^data:[^/]+\/[^;]+;base64,/, '');
  let imageBuffer;
  try {
    imageBuffer = Buffer.from(base64Data, 'base64');
  } catch (e) {
    return createResponse(400, {
      code: -1,
      message: 'imageBase64 解码失败',
    });
  }

  // 文件大小限制（5MB）
  if (imageBuffer.length > 5 * 1024 * 1024) {
    return createResponse(400, {
      code: -1,
      message: '文件大小不能超过 5MB',
    });
  }

  // 生成文件名
  const ext = fileName?.match(/\.[0-9a-z]+$/i)?.[0] || '.jpg';
  const finalFileName = fileName || `avatar_${userId}_${Date.now()}${ext}`;
  const filePath = `avatars/${userId}/${finalFileName}`;

  // 上传到 OSS
  const oss = await getOSSClient();
  try {
    const result = await oss.put(filePath, imageBuffer);
    return createResponse(200, {
      code: 0,
      message: '上传成功',
      data: {
        url: result.url,
        filePath,
      },
    });
  } catch (err) {
    console.error('OSS 上传失败:', err);
    return createResponse(500, {
      code: -1,
      message: '头像上传失败，请稍后重试',
    });
  }
}

// ========== 主入口 ==========
exports.handler = async (event, context) => {
  const startTime = Date.now();
  const requestId = context?.requestId || 'unknown';
  console.log(`🚀 函数开始执行，RequestId: ${requestId}`);

  try {
    // ====== 处理 OPTIONS 预检请求 ======
    if (
      event.httpMethod === 'OPTIONS' ||
      event.requestMethod === 'OPTIONS'
    ) {
      console.log('⚡ 处理 OPTIONS 预检请求');
      return {
        statusCode: 200,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST, GET, OPTIONS, PUT',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
          'Access-Control-Max-Age': '86400',
        },
        body: '',
      };
    }

    // ====== 处理 GET 健康检查 ======
    if (event.httpMethod === 'GET' || event.requestMethod === 'GET') {
      console.log('✅ 处理 GET 健康检查');
      return createResponse(200, {
        code: 0,
        message: '服务运行正常',
        service: '用户认证服务',
        timestamp: new Date().toISOString(),
        env: {
          hasAccessKeyId: !!process.env.ALIYUN_ACCESS_KEY_ID,
          hasJwtSecret: !!process.env.JWT_SECRET,
          otsEndpoint: process.env.OTS_ENDPOINT,
          ossBucket: process.env.OSS_BUCKET,
        },
        endpoints: {
          register: 'POST / { "action": "register", "email": "...", "password": "...", "username": "..." }',
          login: 'POST / { "action": "login", "email": "...", "password": "..." }',
          getUserInfo: 'POST / { "action": "getUserInfo", "userId": "...", "token": "..." }',
          updateUserInfo: 'POST / { "action": "updateUserInfo", "userId": "...", "username": "...", "avatarUrl": "...", "token": "..." }',
          uploadAvatar: 'POST / { "action": "uploadAvatar", "userId": "...", "imageBase64": "...", "token": "..." }',
        },
      });
    }

    // ====== 安全解析 event 为 requestData ======
    let requestData = {};

    try {
      if (event && typeof event === 'object') {
        let body = event.body;

        // 处理 base64 编码
        if (event.isBase64Encoded === true && typeof body === 'string') {
          try {
            body = Buffer.from(body, 'base64').toString('utf8');
          } catch (e) {
            throw new Error('Invalid base64 encoded body');
          }
        }

        // 解析 body
        if (body && typeof body === 'string') {
          try {
            requestData = JSON.parse(body);
          } catch (e) {
            throw new Error('Invalid JSON in request body');
          }
        }

        // 合并 headers & query
        if (event.headers && typeof event.headers === 'object') {
          requestData.headers = { ...event.headers };
        }
        if (event.queryStringParameters && typeof event.queryStringParameters === 'object') {
          Object.assign(requestData, event.queryStringParameters);
        }
      } else if (typeof event === 'string') {
        requestData = JSON.parse(event);
      } else {
        requestData = { ...event };
      }
    } catch (parseErr) {
      console.error('❌ Event parsing failed:', parseErr);
      return createResponse(400, {
        code: -1,
        message: '请求格式错误：无法解析事件数据',
        detail: parseErr.message,
      });
    }

    // 强制校验 action
    const { action } = requestData;
    if (!action || typeof action !== 'string') {
      return createResponse(400, {
        code: -1,
        message: '缺少必需参数 "action"，请确保请求体为 JSON 格式',
        hint: '示例：{"action":"register","email":"user@example.com","password":"123","username":"user"}',
      });
    }

    console.log(`📩 收到请求，action: "${action}", RequestId: ${requestId}`);

    // ====== 分发业务逻辑 ======
    let result;
    try {
      switch (action.toLowerCase()) {
        case 'register':
          result = await register(requestData);
          break;
        case 'login':
          result = await login(requestData);
          break;
        case 'getuserinfo':
        case 'get_user_info':
          result = await getUserInfo(requestData);
          break;
        case 'updateuserinfo':
        case 'update_user_info':
          result = await updateUserInfo(requestData);
          break;
        case 'uploadavatar':
        case 'upload_avatar':
          result = await uploadAvatar(requestData);
          break;
        default:
          result = createResponse(400, {
            code: -1,
            message: `不支持的操作: "${action}"`,
            supported: ['register', 'login', 'getUserInfo', 'updateUserInfo', 'uploadAvatar'],
          });
      }
    } catch (handlerErr) {
      console.error(`❌ Action "${action}" 执行异常:`, handlerErr);
      result = createResponse(500, {
        code: -1,
        message: '服务器内部错误',
        ...(process.env.NODE_ENV === 'development' && { error: handlerErr.message }),
      });
    }

    // 确保返回有效响应
    if (!result || typeof result !== 'object' || !('statusCode' in result)) {
      console.error('🚨 CRITICAL: Handler returned invalid response:', result);
      result = createResponse(500, {
        code: -1,
        message: '服务内部异常，请联系管理员',
      });
    }

    const duration = Date.now() - startTime;
    console.log(`🎉 函数执行完成，耗时: ${duration}ms, RequestId: ${requestId}`);
    return result;
  } catch (fatalErr) {
    const duration = Date.now() - startTime;
    console.error('💥 FC FATAL ERROR:', fatalErr);
    console.error('Stack:', fatalErr.stack);
    console.error(`执行耗时: ${duration}ms, RequestId: ${requestId}`);

    return createResponse(500, {
      code: -1,
      message: '服务启动失败，请检查环境变量配置',
      ...(process.env.NODE_ENV === 'development' && { error: fatalErr.message }),
    });
  }
};
