import java.sql.*;

public class InitMySQL {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String host = env("DB_HOST", "localhost");
            String port = env("DB_PORT", "3306");
            String username = env("DB_USERNAME", "root");
            String password = env("DB_PASSWORD", "");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/mysql?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci",
                username,
                password
            );
            
            Statement stmt = conn.createStatement();
            
            stmt.execute("CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("[OK] Database created");
            
            stmt.execute("GRANT ALL PRIVILEGES ON bizagent.* TO 'root'@'%' WITH GRANT OPTION");
            System.out.println("[OK] Privileges granted");
            
            stmt.execute("FLUSH PRIVILEGES");
            System.out.println("[OK] Privileges flushed");
            
            stmt.close();
            conn.close();
            
            System.out.println("\n[OK] All done! You can now use MySQL with bizagent database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
