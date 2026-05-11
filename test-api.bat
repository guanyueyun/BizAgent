@echo off
chcp 65001 >nul
echo Testing API...

echo 1. Testing GET user list:
curl -s "http://localhost:8080/api/system/user/list?pageNum=1&pageSize=10"
echo.

echo 2. Testing POST create user:
curl -s -X POST "http://localhost:8080/api/system/user" -H "Content-Type: application/json;charset=UTF-8" --data-binary "{\"username\":\"admin\",\"password\":\"123456\",\"realName\":\"\u7ba1\u7406\u5458\",\"email\":\"admin@example.com\"}"
echo.

echo 3. Testing GET user list again:
curl -s "http://localhost:8080/api/system/user/list?pageNum=1&pageSize=10"
echo.

echo 4. Testing AI analyze:
curl -s -X POST "http://localhost:8080/api/ai/analyze" -H "Content-Type: application/json;charset=UTF-8" --data-binary "{\"requirement\":\"\u6211\u8981\u4e00\u4e2a\u8bbe\u5907\u5de1\u68c0\u7ba1\u7406\u6a21\u5757\",\"projectId\":1}"
echo.

echo Test completed!
