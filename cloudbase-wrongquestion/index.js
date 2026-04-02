/**
 * 错题管理云函数
 * 处理错题的保存、删除、查询等操作
 */

// 初始化云开发 SDK
let app = null;
let db = null;

function initCloudBase() {
  if (!app) {
    try {
      const cloud = require('@cloudbase/node-sdk');
      app = cloud.init({
        env: cloud.DYNAMIC_CURRENT_ENV
      });
      db = app.database();
      console.log('✅ 云开发 SDK 初始化成功');
    } catch (error) {
      console.error('❌ 云开发 SDK 初始化失败:', error);
      throw error;
    }
  }
  return { app, db };
}

exports.main = async (event, context) => {
  console.log('📥 收到错题请求:', event.action);

  try {
    // 解析请求数据
    let requestData = event;

    // 如果是通过 HTTP 触发器调用，event 中会有 body 字段
    if (event.body) {
      try {
        if (typeof event.body === 'string') {
          requestData = JSON.parse(event.body);
        } else {
          requestData = event.body;
        }
      } catch (e) {
        console.error('解析 body 失败:', e);
        requestData = event;
      }
    }

    const { action, userId, token, question, questionId } = requestData;

    // 验证用户身份
    if (!userId || !token) {
      return createHttpResponse(401, {
        code: 401,
        message: '未认证：缺少 userId 或 token'
      });
    }

    // 初始化数据库
    const { db } = initCloudBase();

    // ✅ 验证 token 是否有效（查询 tokens 集合）
    const tokens = await db.collection('tokens')
      .where({ token: token, userId: userId })
      .get();

    if (tokens.data.length === 0) {
      return createHttpResponse(401, {
        code: 401,
        message: 'Token 无效或已过期'
      });
    }

    // 检查 token 是否过期
    const tokenData = tokens.data[0];
    if (tokenData.expiresAt < Date.now()) {
      return createHttpResponse(401, {
        code: 401,
        message: 'Token 已过期，请重新登录'
      });
    }

    // ✅ 保存错题
    if (action === 'saveWrongQuestion') {
      const questionData = {
        ...question,
        userId: userId,
        createdAt: question.createdAt || Date.now()
      };

      const result = await db.collection('wrongQuestions').add(questionData);

      return createHttpResponse(200, {
        code: 0,
        message: '错题保存成功',
        data: {
          _id: result.id || result._id,
          ...questionData
        }
      });
    }

    // ✅ 删除错题
    else if (action === 'deleteWrongQuestion') {
      await db.collection('wrongQuestions').doc(questionId).remove();

      return createHttpResponse(200, {
        code: 0,
        message: '错题删除成功'
      });
    }

    // ✅ 获取错题列表
    else if (action === 'getWrongQuestions') {
      const result = await db.collection('wrongQuestions')
        .where({
          userId: userId
        })
        .orderBy('createdAt', 'desc')
        .limit(100)
        .get();

      return createHttpResponse(200, {
        code: 0,
        message: '获取错题列表成功',
        data: result.data || []
      });
    }

    // ❌ 不支持的操作
    else {
      return createHttpResponse(400, {
        code: 400,
        message: `不支持的操作: ${action}`
      });
    }

  } catch (error) {
    console.error('❌ 云函数错误:', error);
    return createHttpResponse(500, {
      code: 500,
      message: '服务器错误: ' + error.message
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
