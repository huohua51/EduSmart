// 初始化云开发 SDK（延迟初始化，避免启动时错误）
let app = null;
let db = null;
let storage = null;

function initCloudBase() {
  if (!app) {
    try {
      const cloud = require('@cloudbase/node-sdk');
      console.log('SDK 版本检查:');
      console.log('- cloud 类型:', typeof cloud);
      console.log('- cloud keys:', Object.keys(cloud || {}));
      console.log('- cloud.init 类型:', typeof cloud.init);
      console.log('- cloud.storage 类型:', typeof cloud.storage);
      
      app = cloud.init({
        env: cloud.DYNAMIC_CURRENT_ENV
      });
      db = app.database();
      
      // 详细检查 app 对象
      console.log('app 对象检查:');
      console.log('- app 类型:', typeof app);
      console.log('- app 是否为 null:', app === null);
      console.log('- app keys:', Object.keys(app || {}));
      console.log('- app.database 类型:', typeof app.database);
      console.log('- app.storage 类型:', typeof app.storage);
      console.log('- app.storage 是否存在:', 'storage' in app);
      
      // 尝试直接使用 cloud.storage() 初始化
      if (typeof cloud.storage === 'function') {
        try {
          storage = cloud.storage({
            env: cloud.DYNAMIC_CURRENT_ENV
          });
          console.log('✅ 使用 cloud.storage() 初始化成功');
        } catch (e) {
          console.warn('⚠️ cloud.storage() 初始化失败:', e.message);
        }
      }
      
      // 初始化 storage
      console.log('开始初始化 storage...');
      console.log('app.storage 类型:', typeof app.storage);
      console.log('app.storage 是否存在:', !!app.storage);
      console.log('app 对象 keys:', Object.keys(app || {}));
      
      try {
        // 尝试多种方式初始化 storage
        if (app.storage && typeof app.storage === 'function') {
          // 方式1：作为函数调用
          storage = app.storage();
          console.log('✅ Storage 初始化成功（函数方式）');
        } else if (app.storage && typeof app.storage === 'object') {
          // 方式2：如果已经是对象，检查是否有 uploadFile 方法
          if (typeof app.storage.uploadFile === 'function') {
            storage = app.storage;
            console.log('✅ Storage 初始化成功（对象方式，有 uploadFile）');
          } else {
            // 尝试调用 getStorage 或类似方法
            if (typeof app.getStorage === 'function') {
              storage = app.getStorage();
              console.log('✅ Storage 初始化成功（通过 getStorage）');
            } else {
              console.warn('⚠️ app.storage 是对象但没有 uploadFile 方法');
              console.warn('app.storage keys:', Object.keys(app.storage || {}));
              storage = null;
            }
          }
        } else {
          console.warn('⚠️ app.storage 不存在或类型不正确');
          storage = null;
        }
        
        // 验证 storage 是否可用
        if (storage) {
          console.log('✅ 云开发 SDK 初始化成功（包含 storage）');
          console.log('storage 类型:', typeof storage);
          console.log('storage 有 uploadFile:', typeof storage.uploadFile === 'function');
        } else {
          console.warn('⚠️ Storage 为 null，可能未开通云存储服务');
        }
      } catch (storageError) {
        console.error('❌ Storage 初始化失败:', storageError);
        console.error('错误堆栈:', storageError.stack);
        storage = null;
      }
    } catch (error) {
      console.error('云开发 SDK 初始化失败:', error);
      throw error;
    }
  }
  return { app, db, storage };
}

/**
 * 从 JSON 字符串中提取指定字段的值
 */
function extractJsonValue(jsonStr, key) {
  const regex = new RegExp(`"${key}"\\s*:\\s*"([^"]+)"`);
  const match = jsonStr.match(regex);
  return match ? match[1] : null;
}

/**
 * 用户注册
 */
async function register(event) {
  try {
    const { email, password, username } = event;
    
    // 参数验证
    if (!email || !password || !username) {
      return {
        code: -1,
        message: '缺少必要参数：email, password, username'
      };
    }
    
    console.log('开始注册，邮箱:', email, '用户名:', username);
    
    // 初始化云开发 SDK
    const { db } = initCloudBase();
    
    // 检查邮箱是否已存在
    const existingUser = await db.collection('users')
      .where({
        email: email
      })
      .get();
    
    if (existingUser.data.length > 0) {
      return {
        code: -1,
        message: '邮箱已被注册'
      };
    }
    
    // 生成用户ID
    const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    // 使用 crypto 加密密码（兼容性更好）
    const crypto = require('crypto');
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');
  
    // 创建用户记录
    const userData = {
      userId: userId,
      username: username,
      email: email,
      password: hashedPassword,
      avatarUrl: null,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    
    console.log('创建用户记录...');
    await db.collection('users').add(userData);
    console.log('用户记录创建成功');
    
    // 生成 Token（使用更安全的随机字符串）
    const token = crypto.randomBytes(32).toString('hex');
    
    // 保存 Token
    console.log('保存 Token...');
    await db.collection('tokens').add({
      userId: userId,
      token: token,
      createdAt: Date.now(),
      expiresAt: Date.now() + 30 * 24 * 60 * 60 * 1000 // 30天
    });
    console.log('Token 保存成功');
    
    return {
      code: 0,
      message: '注册成功',
      data: {
        userId: userId,
        username: username,
        email: email,
        token: token
      }
    };
  } catch (error) {
    console.error('注册函数错误:', error);
    console.error('错误堆栈:', error.stack);
    return {
      code: -1,
      message: '注册失败: ' + (error.message || '未知错误')
    };
  }
}

/**
 * 用户登录
 */
async function login(event) {
  try {
    const { email, password } = event;
    
    // 参数验证
    if (!email || !password) {
      return {
        code: -1,
        message: '缺少必要参数：email, password'
      };
    }
    
    console.log('开始登录，邮箱:', email);
    
    // 初始化云开发 SDK
    const { db } = initCloudBase();
    
    // 查找用户
    const users = await db.collection('users')
      .where({
        email: email
      })
      .get();
    
    if (users.data.length === 0) {
      return {
        code: -1,
        message: '邮箱或密码错误'
      };
    }
    
    const user = users.data[0];
    
    // 使用 crypto 验证密码
    const crypto = require('crypto');
    const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');
    
    if (user.password !== hashedPassword) {
      return {
        code: -1,
        message: '邮箱或密码错误'
      };
    }
  
    // 生成 Token（使用更安全的随机字符串）
    const token = crypto.randomBytes(32).toString('hex');
    
    // 保存 Token
    console.log('保存 Token...');
    await db.collection('tokens').add({
      userId: user.userId,
      token: token,
      createdAt: Date.now(),
      expiresAt: Date.now() + 30 * 24 * 60 * 60 * 1000 // 30天
    });
    console.log('登录成功');
    
    return {
      code: 0,
      message: '登录成功',
      data: {
        userId: user.userId,
        username: user.username,
        email: user.email,
        avatarUrl: user.avatarUrl,
        token: token
      }
    };
  } catch (error) {
    console.error('登录函数错误:', error);
    console.error('错误堆栈:', error.stack);
    return {
      code: -1,
      message: '登录失败: ' + (error.message || '未知错误')
    };
  }
}

/**
 * 获取用户信息
 */
async function getUserInfo(event) {
  const { userId } = event;
  const token = event.headers?.authorization?.replace('Bearer ', '') || event.token;
  
  // 初始化云开发 SDK
  const { db } = initCloudBase();
  
  // 验证 Token
  const tokens = await db.collection('tokens')
    .where({
      token: token,
      userId: userId
    })
    .get();
  
  if (tokens.data.length === 0) {
    return {
      code: -1,
      message: 'Token 无效'
    };
  }
  
  // 检查 Token 是否过期
  const tokenData = tokens.data[0];
  if (tokenData.expiresAt < Date.now()) {
    return {
      code: -1,
      message: 'Token 已过期'
    };
  }
  
  // 获取用户信息
  const users = await db.collection('users')
    .where({
      userId: userId
    })
    .get();
  
  if (users.data.length === 0) {
    return {
      code: -1,
      message: '用户不存在'
    };
  }
  
  const user = users.data[0];
  delete user.password; // 不返回密码
  
  return {
    code: 0,
    message: '获取成功',
    data: user
  };
}

/**
 * 上传头像到云存储
 */
async function uploadAvatar(event) {
  const { userId, image, imageBase64, fileName } = event;
  // 兼容 image 和 imageBase64 两种参数名
  const imageData = image || imageBase64;
  const token = event.headers?.authorization?.replace('Bearer ', '') || event.token;
  
  if (!imageData) {
    return {
      code: -1,
      message: '缺少图片数据'
    };
  }
  
  // 初始化云开发 SDK（只初始化 db，storage 稍后按需初始化）
  const { db } = initCloudBase();
  
  // 验证 Token
  const tokens = await db.collection('tokens')
    .where({
      token: token,
      userId: userId
    })
    .get();
  
  if (tokens.data.length === 0) {
    return {
      code: -1,
      message: 'Token 无效'
    };
  }
  
  // 检查 Token 是否过期
  const tokenData = tokens.data[0];
  if (tokenData.expiresAt < Date.now()) {
    return {
      code: -1,
      message: 'Token 已过期'
    };
  }
  
  try {
    // 使用云开发 SDK 的 storage API（推荐方式）
    const cloudBase = initCloudBase();
    let { storage } = cloudBase;
    
    // 如果 storage 不可用，尝试重新初始化
    if (!storage) {
      console.warn('⚠️ Storage 不可用，尝试重新初始化...');
      const cloud = require('@cloudbase/node-sdk');
      const newApp = cloud.init({
        env: cloud.DYNAMIC_CURRENT_ENV
      });
      
      // 尝试获取 storage
      if (newApp.storage && typeof newApp.storage === 'function') {
        storage = newApp.storage();
        console.log('✅ Storage 重新初始化成功');
      }
    }
    
    // 如果 storage 仍然不可用，使用备用方案：将头像存储到数据库
    if (!storage || typeof storage.uploadFile !== 'function') {
      console.warn('⚠️ Storage 不可用，使用备用方案：将头像存储到数据库');
      
      // 将 base64 数据直接存储到数据库中
      // 注意：这不是最佳实践，但可以作为临时方案
      const avatarDataUrl = `data:image/jpeg;base64,${imageData}`;
      
      // 更新用户的头像字段
      const users = await db.collection('users')
        .where({ userId: userId })
        .get();
      
      if (users.data.length > 0) {
        const userDocId = users.data[0]._id;
        await db.collection('users').doc(userDocId).update({
          avatarUrl: avatarDataUrl,
          updatedAt: Date.now()
        });
        
        console.log('✅ 头像已保存到数据库（Base64 格式）');
        
        return {
          code: 0,
          message: '上传成功（备用方案）',
          data: {
            url: avatarDataUrl
          }
        };
      } else {
        return {
          code: -1,
          message: '用户不存在'
        };
      }
    }
    
    // 使用 storage 上传
    const finalFileName = fileName || `avatar_${Date.now()}.jpg`;
    const filePath = `avatars/${userId}/${finalFileName}`;
    
    // 将 base64 转换为 Buffer
    const imageBuffer = Buffer.from(imageData, 'base64');
    console.log('准备上传文件，路径:', filePath, '大小:', imageBuffer.length, 'bytes');
    
    const uploadResult = await storage.uploadFile({
      cloudPath: filePath,
      fileContent: imageBuffer
    });
    
    console.log('文件上传成功，fileID:', uploadResult.fileID);
    
    // 获取文件下载链接
    const downloadUrl = await storage.getTempFileURL({
      fileList: [filePath]
    });
    
    console.log('获取下载链接结果:', downloadUrl);
    
    if (downloadUrl.fileList && downloadUrl.fileList.length > 0 && downloadUrl.fileList[0].tempFileURL) {
      return {
        code: 0,
        message: '上传成功',
        data: {
          fileId: uploadResult.fileID,
          url: downloadUrl.fileList[0].tempFileURL,
          cloudPath: filePath
        }
      };
    } else {
      const cloud = require('@cloudbase/node-sdk');
      const envId = cloud.DYNAMIC_CURRENT_ENV || 'edusmart-dev-3gqo04ike66344ea-1327750873';
      const fallbackUrl = `https://${envId}.tcb.qcloud.la/${uploadResult.fileID}`;
      
      return {
        code: 0,
        message: '上传成功',
        data: {
          fileId: uploadResult.fileID,
          url: fallbackUrl,
          cloudPath: filePath
        }
      };
    }
  } catch (error) {
    console.error('上传头像错误:', error);
    console.error('错误堆栈:', error.stack);
    return {
      code: -1,
      message: error.message || '上传失败'
    };
  }
}

/**
 * 同步错题到云端
 * 用于客户端将本地添加的错题同步到云数据库
 */
async function syncWrongQuestion(event) {
  try {
    const { userId, token, question, attachmentsBase64 = [], localId } = event;
    if (!userId || !question) {
      return { code: -1, message: '缺少必要参数：userId 或 question' };
    }

    // 初始化 SDK
    const { db, storage } = initCloudBase();

    // 验证 token（复用现有 token 验证逻辑）
    const tokens = await db.collection('tokens')
      .where({ token: token, userId: userId })
      .get();

    if (tokens.data.length === 0) {
      return { code: -1, message: 'Token 无效' };
    }
    const tokenData = tokens.data[0];
    if (tokenData.expiresAt < Date.now()) {
      return { code: -1, message: 'Token 已过期' };
    }

    // 处理附件上传
    const uploadedAttachments = [];

    if (attachmentsBase64 && attachmentsBase64.length > 0) {
      if (!storage || typeof storage.uploadFile !== 'function') {
        console.warn('⚠️ Storage 不可用，跳过附件上传');
      } else {
        for (let i = 0; i < attachmentsBase64.length; i++) {
          try {
            const item = attachmentsBase64[i];
            let base64Str, fileName;
            if (typeof item === 'string') {
              base64Str = item;
              fileName = `wrong_${Date.now()}_${i}.jpg`;
            } else {
              base64Str = item.base64;
              fileName = item.fileName || `wrong_${Date.now()}_${i}.jpg`;
            }

            const cloudPath = `wrong_questions/${userId}/${Date.now()}_${fileName}`;
            const buffer = Buffer.from(base64Str, 'base64');

            const uploadResult = await storage.uploadFile({
              cloudPath: cloudPath,
              fileContent: buffer
            });

            let url = null;
            try {
              const tempRes = await storage.getTempFileURL({ fileList: [cloudPath] });
              if (tempRes.fileList && tempRes.fileList[0] && tempRes.fileList[0].tempFileURL) {
                url = tempRes.fileList[0].tempFileURL;
              }
            } catch (e) {
              url = `fileID:${uploadResult.fileID}`;
            }

            uploadedAttachments.push({
              fileId: uploadResult.fileID,
              url: url,
              cloudPath: cloudPath,
              fileName: fileName
            });
            console.log('✅ 单个附件上传成功:', fileName);
          } catch (e) {
            console.warn('⚠️ 上传单个附件失败，跳过:', e.message);
          }
        }
      }
    }

    // 构造文档并写入 wrong_questions collection
    const doc = {
      userId: userId,
      question: question,
      attachments: uploadedAttachments,
      localId: localId,  // 记录本地ID用于关联
      source: event.source || 'app',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      visibility: event.visibility || 'private'
    };

    const addRes = await db.collection('wrong_questions').add(doc);
    const returnedId = addRes.id || addRes._id || addRes.insertedId || null;

    console.log('✅ 错题同步成功，ID:', returnedId);

    return {
      code: 0,
      message: '同步成功',
      data: {
        id: returnedId,
        createdAt: doc.createdAt
      }
    };
  } catch (error) {
    console.error('❌ syncWrongQuestion 错误:', error);
    return { code: -1, message: '同步失败: ' + (error.message || '未知错误') };
  }
}

/**
 * 更新用户信息
 */
async function updateUserInfo(event) {
  const { userId, username, avatarUrl } = event;
  const token = event.headers?.authorization?.replace('Bearer ', '') || event.token;
  
  // 初始化云开发 SDK
  const { db } = initCloudBase();
  
  // 验证 Token
  const tokens = await db.collection('tokens')
    .where({
      token: token,
      userId: userId
    })
    .get();
  
  if (tokens.data.length === 0) {
    return {
      code: -1,
      message: 'Token 无效'
    };
  }
  
  // 检查 Token 是否过期
  const tokenData = tokens.data[0];
  if (tokenData.expiresAt < Date.now()) {
    return {
      code: -1,
      message: 'Token 已过期'
    };
  }
  
  // 获取用户信息
  const users = await db.collection('users')
    .where({
      userId: userId
    })
    .get();
  
  if (users.data.length === 0) {
    return {
      code: -1,
      message: '用户不存在'
    };
  }
  
  const userDoc = users.data[0];
  const userDocId = users.data[0]._id;
  
  // 构建更新数据
  const updateData = {
    updatedAt: Date.now()
  };
  
  if (username !== undefined && username !== null) {
    updateData.username = username;
  }
  
  if (avatarUrl !== undefined && avatarUrl !== null) {
    updateData.avatarUrl = avatarUrl;
  }
  
  // 更新用户信息
  await db.collection('users').doc(userDocId).update(updateData);
  
  // 获取更新后的用户信息
  const updatedUser = await db.collection('users').doc(userDocId).get();
  const user = updatedUser.data[0];
  delete user.password; // 不返回密码
  
  return {
    code: 0,
    message: '更新成功',
    data: user
  };
}

/**
 * 主函数（普通函数 + HTTP 触发器）
 * 通过 HTTP 触发器调用时，需要返回 HTTP 响应格式
 */
// 主函数入口（兼容普通函数和 HTTP 触发器）
exports.main = async (event, context) => {
  console.log('========== 云函数开始执行 ==========');
  console.log('Event type:', typeof event);
  console.log('Event keys:', Object.keys(event || {}));
  console.log('Event.body type:', typeof event.body);
  console.log('Event.body length:', event.body ? (typeof event.body === 'string' ? event.body.length : JSON.stringify(event.body).length) : 0);
  
  try {
    // 初始化云开发 SDK（延迟初始化，只在需要时初始化）
    // 不在入口处初始化，避免 storage 不存在的问题
    // 解析请求数据
    let requestData = event;
    
    // 如果是通过 HTTP 触发器调用，event 中会有 body 字段
    if (event.body) {
      try {
        if (typeof event.body === 'string') {
          // 如果是字符串，尝试解析 JSON
          // 对于大文件，可能只解析前几KB来获取 action
          const bodyStr = event.body;
          console.log('Body 是字符串，长度:', bodyStr.length);
          
          // 尝试解析完整 JSON
          try {
            requestData = JSON.parse(bodyStr);
            console.log('解析完整 body 成功');
          } catch (parseError) {
            // 如果解析失败（可能因为太大），尝试提取 action
            console.warn('完整 body 解析失败，尝试提取 action:', parseError.message);
            
            // 尝试从字符串中提取 action（查找 "action":"uploadAvatar"）
            const actionMatch = bodyStr.match(/"action"\s*:\s*"([^"]+)"/);
            if (actionMatch) {
              const action = actionMatch[1];
              console.log('从 body 字符串中提取到 action:', action);
              
              // 对于 uploadAvatar，需要特殊处理大文件
              if (action === 'uploadAvatar') {
                // 尝试解析，但限制大小
                try {
                  requestData = JSON.parse(bodyStr);
                } catch (e) {
                  // 如果还是失败，手动构建请求数据
                  console.warn('JSON 解析失败，手动构建请求数据');
                  requestData = {
                    action: action,
                    userId: extractJsonValue(bodyStr, 'userId'),
                    imageBase64: bodyStr.match(/"imageBase64"\s*:\s*"([^"]+)"/)?.[1] || 
                                bodyStr.match(/"imageBase64"\s*:\s*"([^"]{0,100})/)?.[1], // 只取前100个字符用于调试
                    token: event.headers?.authorization?.replace('Bearer ', '') || extractJsonValue(bodyStr, 'token')
                  };
                  console.log('手动构建的 requestData keys:', Object.keys(requestData));
                }
              } else {
                requestData = JSON.parse(bodyStr);
              }
            } else {
              throw new Error('无法从 body 中提取 action');
            }
          }
        } else {
          // 如果已经是对象，直接使用
          requestData = event.body;
          console.log('Body 是对象，直接使用');
        }
        console.log('解析后的 requestData keys:', Object.keys(requestData || {}));
        console.log('Action:', requestData?.action);
      } catch (e) {
        console.error('解析 body 失败:', e);
        console.error('错误堆栈:', e.stack);
        requestData = event;
      }
    }
    
    // 合并 headers（用于获取 Authorization）
    if (event.headers) {
      requestData.headers = event.headers;
      // 从 headers 中提取 token（如果 body 中没有）
      if (!requestData.token && event.headers.authorization) {
        requestData.token = event.headers.authorization.replace('Bearer ', '');
      }
    }
    
    const { action } = requestData || {};
    console.log('最终 Action:', action);
    console.log('requestData 类型:', typeof requestData);
    console.log('requestData 是否为 null:', requestData === null);
    
    if (!action) {
      console.error('缺少 action 参数');
      console.error('requestData:', JSON.stringify(requestData, null, 2).substring(0, 500));
      return createHttpResponse(400, {
        code: -1,
        message: '缺少 action 参数，请检查请求格式'
      });
    }
    
    // 调用对应的处理函数
    let result;
    try {
      switch (action) {
        case 'register':
          console.log('调用 register 函数');
          result = await register(requestData);
          break;
        case 'login':
          console.log('调用 login 函数');
          result = await login(requestData);
          break;
        case 'getUserInfo':
          console.log('调用 getUserInfo 函数');
          result = await getUserInfo(requestData);
          break;
        case 'updateUserInfo':
          console.log('调用 updateUserInfo 函数');
          result = await updateUserInfo(requestData);
          break;
        case 'uploadAvatar':
          console.log('调用 uploadAvatar 函数');
          result = await uploadAvatar(requestData);
          break;
        case 'createNote':
          console.log('调用 createNote 函数');
          result = await createNote(requestData);
          break;
        case 'updateNote':
          console.log('调用 updateNote 函数');
          result = await updateNote(requestData);
          break;
        case 'deleteNote':
          console.log('调用 deleteNote 函数');
          result = await deleteNote(requestData);
          break;
        case 'getNotes':
          console.log('调用 getNotes 函数');
          result = await getNotes(requestData);
          break;
        case 'getNoteById':
          console.log('调用 getNoteById 函数');
          result = await getNoteById(requestData);
          break;
        case 'uploadNoteFile':
          console.log('调用 uploadNoteFile 函数');
          result = await uploadNoteFile(requestData);
          break;
        case 'syncWrongQuestion':
          console.log('调用 syncWrongQuestion 函数');
          result = await syncWrongQuestion(requestData);
          break;
        default:
          console.error('未知操作:', action);
          result = {
            code: -1,
            message: '未知操作: ' + action
          };
      }
    } catch (funcError) {
      console.error('处理函数执行错误:', funcError);
      console.error('错误堆栈:', funcError.stack);
      result = {
        code: -1,
        message: '函数执行失败: ' + (funcError.message || '未知错误')
      };
    }
    
    console.log('返回结果:', JSON.stringify(result, null, 2));
    console.log('========== 云函数执行完成 ==========');
    
    // 返回 HTTP 响应格式
    return createHttpResponse(result.code === 0 ? 200 : 400, result);
  } catch (error) {
    console.error('========== 云函数致命错误 ==========');
    console.error('错误信息:', error);
    console.error('错误堆栈:', error.stack);
    return createHttpResponse(500, {
      code: -1,
      message: '服务器错误: ' + (error.message || '未知错误')
    });
  }
};

/**
 * 创建 HTTP 响应
 */
function createHttpResponse(statusCode, data) {
  return {
    statusCode: statusCode,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    },
    body: JSON.stringify(data)
  };
}


