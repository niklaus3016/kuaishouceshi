# 福利抽奖API接口文档

## 1. 获取福利抽奖信息

### 接口说明
获取用户的福利钱包余额和抽奖机会

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/lottery/info`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "balance": 1280.50,
      "chances": 3
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取信息失败"
  }
  ```

## 2. 获取福利抽奖奖品列表

### 接口说明
获取福利抽奖的奖品列表及其中奖概率

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/lottery/prizes`

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "prizes": [
        {
          "id": "1",
          "name": "100元现金",
          "value": 100,
          "type": "cash",
          "probability": 5
        },
        {
          "id": "2",
          "name": "50元现金",
          "value": 50,
          "type": "cash",
          "probability": 10
        },
        {
          "id": "3",
          "name": "20元现金",
          "value": 20,
          "type": "cash",
          "probability": 15
        },
        {
          "id": "4",
          "name": "10元现金",
          "value": 10,
          "type": "cash",
          "probability": 20
        },
        {
          "id": "5",
          "name": "5元现金",
          "value": 5,
          "type": "cash",
          "probability": 25
        },
        {
          "id": "6",
          "name": "手机",
          "value": 5000,
          "type": "phone",
          "probability": 1
        },
        {
          "id": "7",
          "name": "金条",
          "value": 2000,
          "type": "gold",
          "probability": 2
        },
        {
          "id": "8",
          "name": "再接再厉",
          "value": 0,
          "type": "encourage",
          "probability": 22
        }
      ]
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取奖品列表失败"
  }
  ```

## 3. 领取福利抽奖

### 接口说明
用户进行福利抽奖，返回抽奖结果

### 请求信息
- **方法**: POST
- **URL**: `/api/welfare/lottery/claim`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "result": {
        "id": "1",
        "name": "100元现金",
        "value": 100,
        "type": "cash"
      }
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "抽奖失败"
  }
  ```

## 4. 获取福利抽奖记录

### 接口说明
获取用户的福利抽奖历史记录

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/lottery/records`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "records": [
        {
          "id": "1",
          "time": "2026-04-13T22:44:00Z",
          "name": "100元现金",
          "value": 100,
          "type": "cash"
        },
        {
          "id": "2",
          "time": "2026-04-13T21:44:00Z",
          "name": "50元现金",
          "value": 50,
          "type": "cash"
        }
      ]
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取记录失败"
  }
  ```

## 5. 福利钱包提现

### 接口说明
用户从福利钱包提现到支付宝

### 请求信息
- **方法**: POST
- **URL**: `/api/welfare/withdraw`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |
  | amount | number | 是 | 提现金额 |
  | alipayAccount | string | 是 | 支付宝账号 |
  | alipayName | string | 是 | 支付宝姓名 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "提现申请已提交，等待处理",
    "data": {
      "success": true
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "提现失败"
  }
  ```

## 6. 获取福利钱包余额

### 接口说明
获取用户的福利钱包余额

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/wallet/balance`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "balance": 1280.50
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取余额失败"
  }
  ```

## 7. 获取提现记录

### 接口说明
获取用户的福利钱包提现历史记录

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/withdraw/records`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "records": [
        {
          "id": "1",
          "time": "2026-04-13T22:44:00Z",
          "amount": 100,
          "status": "processing",
          "statusText": "处理中",
          "statusColor": "text-blue-400"
        },
        {
          "id": "2",
          "time": "2026-04-12T21:44:00Z",
          "amount": 50,
          "status": "completed",
          "statusText": "已到账",
          "statusColor": "text-green-400"
        }
      ]
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取提现记录失败"
  }
  ```

## 8. 绑定支付宝信息

### 接口说明
用户绑定支付宝信息，用于福利钱包提现

### 请求信息
- **方法**: POST
- **URL**: `/api/welfare/bind-alipay`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |
  | alipayName | string | 是 | 支付宝姓名 |
  | alipayAccount | string | 是 | 支付宝账号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "message": "绑定成功",
    "data": {
      "alipayName": "张三",
      "alipayAccount": "zhangsan@example.com"
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "绑定失败"
  }
  ```

## 9. 获取绑定的支付宝信息

### 接口说明
获取用户已绑定的支付宝信息

### 请求信息
- **方法**: GET
- **URL**: `/api/welfare/get-alipay`
- **参数**:
  | 参数名 | 类型 | 必填 | 描述 |
  |--------|------|------|------|
  | userId | string | 是 | 用户ID |
  | employeeId | string | 是 | 员工号 |

### 响应信息
- **成功响应**:
  ```json
  {
    "success": true,
    "data": {
      "alipayName": "张三",
      "alipayAccount": "zhangsan@example.com"
    }
  }
  ```
- **失败响应**:
  ```json
  {
    "success": false,
    "message": "获取支付宝信息失败"
  }
  ```