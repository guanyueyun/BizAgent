import mysql.connector
import os

try:
    connection = mysql.connector.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', '3306')),
        user=os.getenv('DB_USERNAME', 'root'),
        password=os.getenv('DB_PASSWORD', '')
    )
    
    cursor = connection.cursor()
    cursor.execute("CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    cursor.execute("GRANT ALL PRIVILEGES ON bizagent.* TO 'root'@'%'")
    cursor.execute("FLUSH PRIVILEGES")
    
    print("[OK] 数据库创建成功！")
    
    cursor.close()
    connection.close()
except Exception as e:
    print(f"[ERROR] 错误: {e}")
