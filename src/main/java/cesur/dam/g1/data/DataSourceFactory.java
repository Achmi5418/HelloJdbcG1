package cesur.dam.g1.data;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;

/**
 * Fábrica encargada de crear y proporcionar un {@link DataSource} para la
 * base de datos MySQL.
 * <p>
 * Implementa un patrón singleton perezoso (lazy singleton): el
 * {@link DataSource} se crea la primera vez que se invoca
 * {@link #getDataSource()} y se reutiliza en llamadas posteriores.
 * Las credenciales se obtienen de las variables de entorno {@code USER} y
 * {@code PASSWORD}.
 * </p>
 */
public class DataSourceFactory {

    /** URL de conexión a la base de datos MySQL. */
    private static String url = "jdbc:mysql://localhost:3309/ad";

    /** Nombre de usuario obtenido de la variable de entorno {@code USER}. */
    private static String username = System.getenv("USER") ;

    /** Contraseña obtenida de la variable de entorno {@code PASSWORD}. */
    private static String password = System.getenv("PASSWORD") ;

    /** Instancia única del {@link DataSource}, creada de forma perezosa. */
    private static DataSource dataSource = null;

    /**
     * Devuelve el {@link DataSource} compartido, creándolo si aún no existe.
     *
     * @return el {@link DataSource} configurado para conectar con MySQL
     * @throws RuntimeException si las variables de entorno {@code USER} o
     *                          {@code PASSWORD} no están definidas
     */
    public static DataSource getDataSource() {
        if(dataSource == null) {
            if(username == null || password == null) {
                throw new RuntimeException("Username and/or password are mandatory");
            }
            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(url);
            ds.setUser(username);
            ds.setPassword(password);
            dataSource=ds;
        }
        return dataSource;
    }
}
