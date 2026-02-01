package dbbrowser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//@author Berk
public class DatabaseConnection {
    
    private static final String URL = "jdbc:mysql://localhost:3306/CourseRegistration?useUnicode=true&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // XAMPP password empty

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("BAŞARILI: Veritabanına bağlandı!");
            
        } catch (ClassNotFoundException e) {
            System.out.println("HATA: Driver bulunamadı! Kütüphane ekleme adımını kontrol et.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("HATA: Bağlantı kurulamadı! XAMPP açık mı?");
            e.printStackTrace();
        }
        return conn;
    }
}
