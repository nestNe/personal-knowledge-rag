# ChatController 接口文档

## 1. 接口概览

ChatController 提供AI对话相关的接口，包括基本聊天、带知识库和记忆功能的聊天、会话管理等功能。

## 2. 接口列表

| 接口名称 | 请求方式 | 请求路径 | 功能描述 | 权限要求 |
| :--- | :--- | :--- | :--- | :--- |
| 基本聊天 | POST | /api/ai/chat | 发送消息并获取AI回复 | agent:chat |
| 知识库聊天 | POST | /api/ai/chat/kb | 带记忆和知识库功能的聊天 | agent:chat |
| 获取会话列表 | GET | /api/ai/sessions | 获取当前用户的会话列表 | agent:chat |
| 获取会话详情 | GET | /api/ai/sessions/{sessionId}/messages | 获取单个会话的详细聊天记录 | agent:chat |

## 3. 详细接口说明

### 3.1 基本聊天

**请求方式**: POST
**请求路径**: /api/ai/chat
**功能描述**: 发送消息并获取AI回复
**权限要求**: agent:chat

**请求参数**:

| 参数名 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| message | String | 是 | 聊天消息内容 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/ai/chat?message=你好，我想了解一下系统功能" \
  -H "Authorization: Bearer {token}"
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": "你好！我是系统助手，可以帮你了解系统功能。系统主要包括用户管理、角色管理、权限管理和AI对话功能。你具体想了解哪个功能呢？",
  "timestamp": 1712119200000
}
```

### 3.2 知识库聊天

**请求方式**: POST
**请求路径**: /api/ai/chat/kb
**功能描述**: 带记忆和知识库功能的聊天，支持会话管理
**权限要求**: agent:chat

**请求参数**:

| 参数名 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| message | String | 是 | 聊天消息内容 |
| sessionId | String | 否 | 会话ID，不提供则创建新会话 |

**请求示例**:
```bash
curl -X POST "http://localhost:8080/api/ai/chat/kb?message=什么是用户管理&sessionId=123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer {token}"
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": "用户管理是系统的核心功能之一，主要包括用户的创建、编辑、删除、角色分配等操作。通过用户管理，管理员可以控制系统的访问权限，确保只有授权用户能够使用系统功能。",
  "timestamp": 1712119200000
}
```

### 3.3 获取会话列表

**请求方式**: GET
**请求路径**: /api/ai/sessions
**功能描述**: 获取当前用户的所有会话列表
**权限要求**: agent:chat

**请求参数**: 无

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/ai/sessions" \
  -H "Authorization: Bearer {token}"
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "sessionId": "123e4567-e89b-12d3-a456-426614174000",
      "userId": 1,
      "title": "系统功能咨询",
      "summary": "关于系统功能的咨询",
      "modelType": "gpt-4",
      "isActive": true,
      "messageCount": 5,
      "lastMessageTime": "2026-04-03T10:15:00",
      "createdAt": "2026-04-03T10:00:00",
      "updatedAt": "2026-04-03T10:15:00"
    },
    {
      "id": 2,
      "sessionId": "234e5678-e9ab-23d4-b567-537725285111",
      "userId": 1,
      "title": "用户管理问题",
      "summary": "用户管理相关问题",
      "modelType": "gpt-4",
      "isActive": true,
      "messageCount": 3,
      "lastMessageTime": "2026-04-03T11:05:00",
      "createdAt": "2026-04-03T11:00:00",
      "updatedAt": "2026-04-03T11:05:00"
    }
  ],
  "timestamp": 1712119200000
}
```

### 3.4 获取会话详情

**请求方式**: GET
**请求路径**: /api/ai/sessions/{sessionId}/messages
**功能描述**: 获取单个会话的详细聊天记录
**权限要求**: agent:chat

**路径参数**:

| 参数名 | 类型 | 必填 | 描述 |
| :--- | :--- | :--- | :--- |
| sessionId | String | 是 | 会话ID |

**请求示例**:
```bash
curl -X GET "http://localhost:8080/api/ai/sessions/123e4567-e89b-12d3-a456-426614174000/messages" \
  -H "Authorization: Bearer {token}"
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "chatSession": {
      "id": 1,
      "sessionId": "123e4567-e89b-12d3-a456-426614174000",
      "userId": 1,
      "title": "系统功能咨询",
      "summary": "关于系统功能的咨询",
      "modelType": "gpt-4",
      "isActive": true,
      "messageCount": 5,
      "lastMessageTime": "2026-04-03T10:15:00",
      "createdAt": "2026-04-03T10:00:00",
      "updatedAt": "2026-04-03T10:15:00"
    },
    "chatMemories": [
      {
        "conversationId": "123e4567-e89b-12d3-a456-426614174000",
        "content": "你好，我想了解一下系统功能",
        "type": "USER",
        "timestamp": "2026-04-03T10:00:00"
      },
      {
        "conversationId": "123e4567-e89b-12d3-a456-426614174000",
        "content": "你好！我是系统助手，可以帮你了解系统功能。系统主要包括用户管理、角色管理、权限管理和AI对话功能。你具体想了解哪个功能呢？",
        "type": "ASSISTANT",
        "timestamp": "2026-04-03T10:00:10"
      }
    ]
  },
  "timestamp": 1712119200000
}
```

## 4. 响应格式说明

所有接口的响应格式统一为以下结构：

```json
{
  "code": 200, // 状态码，200表示成功
  "message": "success", // 响应消息
  "data": {}, // 响应数据，根据接口不同而不同
  "timestamp": 1712119200000 // 响应时间戳
}
```

## 5. 错误响应

当请求失败时，响应格式如下：

```json
{
  "code": 400, // 错误状态码
  "message": "错误消息", // 错误描述
  "data": null,
  "timestamp": 1712119200000
}
```

常见错误状态码：
- 400: 请求参数错误
- 401: 未授权，token无效或过期
- 403: 禁止访问，权限不足
- 404: 资源不存在
- 500: 服务器内部错误

## 6. 接口调用示例

### 6.1 使用curl调用基本聊天接口

```bash
curl -X POST "http://localhost:8080/api/ai/chat?message=如何创建新用户" \
  -H "Authorization: Bearer {token}"
```

### 6.2 使用curl调用知识库聊天接口

```bash
curl -X POST "http://localhost:8080/api/ai/chat/kb?message=什么是角色管理&sessionId=123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer {token}"
```

### 6.3 使用curl获取会话列表

```bash
curl -X GET "http://localhost:8080/api/ai/sessions" \
  -H "Authorization: Bearer {token}"
```

## 7. 注意事项

1. 所有接口都需要在请求头中携带有效的token
2. 聊天接口需要agent:chat权限
3. 知识库聊天接口会自动管理会话，不提供sessionId时会创建新会话
4. 聊天记录会自动保存，每3条消息会生成一次会话摘要
5. 会话详情接口只返回当前用户的会话数据，无法访问其他用户的会话
6. 由于AI处理需要时间，聊天接口可能响应较慢，建议前端设置较长的超时时间