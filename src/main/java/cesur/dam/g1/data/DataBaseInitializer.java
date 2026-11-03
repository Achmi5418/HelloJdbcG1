package cesur.dam.g1.data;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DataBaseInitializer {

    /**
        Método que se encarga de inicializar la tabla en la base de datos.
        Usa el script init.sql
    */
    public static void initialize(DataSource ds){
        executeScript(ds, "init.sql");
    }

    /**
        Método que carga los datos iniciales en la base de datos si la tabla
        está vacía. Usa el script data.sql
    */
    public static void loadData(DataSource ds){
        String countSql = "select count(*) from Equipo";

        try (Connection conn = ds.getConnection();
             PreparedStatement pst = conn.prepareStatement(countSql);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                executeScript(ds, "data.sql");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
        Ejecuta todas las sentencias de un script SQL ubicado en los recursos.
    */
    private static void executeScript(DataSource ds, String resourceName){
        try (InputStream is = DataBaseInitializer.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException("No se encontró el recurso " + resourceName);
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            try (Connection conn = ds.getConnection();
                 Statement st = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    if (!statement.isBlank()) {
                        st.execute(statement);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
