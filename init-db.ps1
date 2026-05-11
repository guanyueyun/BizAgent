try {
    Add-Type -Path "C:\Program Files\MySQL\Connector NET 8.0\Assemblies\v4.8\MySqlConnector.dll" -ErrorAction SilentlyContinue

    $dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
    $dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "3306" }
    $dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
    $dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "" }
    $connectionString = "server=$dbHost;port=$dbPort;uid=$dbUser;pwd=$dbPassword;"
    $connection = New-Object MySqlConnector.MySqlConnection($connectionString)
    $connection.Open()
    
    $command = $connection.CreateCommand()
    $command.CommandText = "CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON bizagent.* TO 'root'@'%'; FLUSH PRIVILEGES;"
    $command.ExecuteNonQuery()
    
    Write-Host "[OK] 数据库创建并授权成功！"
    
    $connection.Close()
} catch {
    Write-Host "[ERROR] 错误: $_"
}
