import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitDB {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String host = env("DB_HOST", "localhost");
            String port = env("DB_PORT", "3306");
            String username = env("DB_USERNAME", "root");
            String password = env("DB_PASSWORD", "");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci",
                username,
                password
            );
            
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS bizagent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("GRANT ALL PRIVILEGES ON bizagent.* TO 'root'@'%'");
            stmt.execute("FLUSH PRIVILEGES");
            
            stmt.close();
            conn.close();
            System.out.println("[OK] Database created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
