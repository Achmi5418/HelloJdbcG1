package cesur.dam.g1.data;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DataBaseInitializer {

    /**
        Método que se encarga de inicializar la tabla en la base de datos.
        Usa el script init.sql
    */
    public static void initialize(DataSource ds){
        try (InputStream is = DataBaseInitializer.class.getClassLoader().getResourceAsStream("init.sql")) {
            if (is == null) {
                throw new RuntimeException("No se encontró el recurso init.sql");
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            try (Connection conn = ds.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadData(DataSource ds){

    };

}
