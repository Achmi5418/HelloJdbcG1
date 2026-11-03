package cesur.dam.g1.data;

import cesur.dam.g1.models.Equipo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link TransactionalDAO} para {@link Equipo} basada en
 * JDBC.
 * <p>
 * Dispone de dos constructores sobrecargados:
 * </p>
 * <ul>
 *     <li>{@link #EquipoJdbcTransactionalDAO(DataSource)}: los métodos sin
 *     conexión abren y cierran su propia {@link Connection}.</li>
 *     <li>{@link #EquipoJdbcTransactionalDAO(Connection)}: los métodos sin
 *     conexión reutilizan la conexión inyectada, lo que permite participar en
 *     una transacción gestionada por el llamador.</li>
 * </ul>
 * <p>
 * Los métodos que reciben una {@link Connection} nunca la cierran ni ejecutan
 * {@code commit} o {@code rollback}.
 * </p>
 */
public class EquipoJdbcTransactionalDAO implements TransactionalDAO<Equipo> {

    /** Origen de datos usado cuando no se inyecta una conexión. */
    private final DataSource ds;

    /** Conexión externa reutilizada por los métodos sin conexión, si existe. */
    private final Connection connection;

    /**
     * Crea un DAO que abrirá sus propias conexiones a partir del
     * {@link DataSource}.
     *
     * @param ds origen de datos para obtener las conexiones
     */
    public EquipoJdbcTransactionalDAO(DataSource ds) {
        this.ds = ds;
        this.connection = null;
    }

    /**
     * Crea un DAO que reutilizará la conexión indicada en los métodos sin
     * conexión, permitiendo trabajar dentro de una transacción externa.
     *
     * @param connection conexión gestionada por el llamador
     */
    public EquipoJdbcTransactionalDAO(Connection connection) {
        this.ds = null;
        this.connection = connection;
    }

    /* ===== Lecturas ===== */

    /**
     * {@inheritDoc}
     * <p>
     * Si el DAO se construyó con una conexión, la reutiliza; en caso contrario
     * abre una nueva a partir del {@link DataSource} y la cierra al terminar.
     * </p>
     */
    @Override
    public Optional<Equipo> findById(Long id) {
        if (connection != null) {
            return findById(connection, id);
        }
        try (Connection conn = ds.getConnection()) {
            return findById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Equipo> findById(Connection conn, Long id) {
        String sql = "select * from Equipo where id=?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, id);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(EquipoJdbcDAO.mapper(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Si el DAO se construyó con una conexión, la reutiliza; en caso contrario
     * abre una nueva a partir del {@link DataSource} y la cierra al terminar.
     * </p>
     */
    @Override
    public List<Equipo> findAll() {
        if (connection != null) {
            return findAll(connection);
        }
        try (Connection conn = ds.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Equipo> findAll(Connection conn) {
        String sql = "select * from Equipo";
        List<Equipo> equipos = new ArrayList<>();

        try (PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                equipos.add(EquipoJdbcDAO.mapper(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return equipos;
    }

    /* ===== Escrituras ===== */

    /**
     * {@inheritDoc}
     * <p>
     * Si el DAO se construyó con una conexión, la reutiliza; en caso contrario
     * abre una nueva a partir del {@link DataSource} y la cierra al terminar.
     * </p>
     */
    @Override
    public Equipo save(Equipo obj) {
        if (connection != null) {
            return save(connection, obj);
        }
        try (Connection conn = ds.getConnection()) {
            return save(conn, obj);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Equipo save(Connection conn, Equipo obj) {
        String sql = "insert into Equipo (nombre, categoria, disponible) " +
                     "VALUES (?,?,?)";

        try (PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, obj.getNombre());
            pst.setString(2, obj.getCategoria());
            pst.setBoolean(3, obj.getDisponible());
            pst.executeUpdate();

            try (ResultSet rs = pst.getGeneratedKeys()) {
                if (rs.next()) {
                    obj.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Si el DAO se construyó con una conexión, la reutiliza; en caso contrario
     * abre una nueva a partir del {@link DataSource} y la cierra al terminar.
     * </p>
     */
    @Override
    public void update(Equipo entity) {
        if (connection != null) {
            update(connection, entity);
            return;
        }
        try (Connection conn = ds.getConnection()) {
            update(conn, entity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Connection conn, Equipo entity) {
        String sql = "update Equipo set nombre=?, categoria=?, disponible=? " +
                     "where id=?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, entity.getNombre());
            pst.setString(2, entity.getCategoria());
            pst.setBoolean(3, entity.getDisponible());
            pst.setLong(4, entity.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Si el DAO se construyó con una conexión, la reutiliza; en caso contrario
     * abre una nueva a partir del {@link DataSource} y la cierra al terminar.
     * </p>
     */
    @Override
    public void deleteById(Long id) {
        if (connection != null) {
            deleteById(connection, id);
            return;
        }
        try (Connection conn = ds.getConnection()) {
            deleteById(conn, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Connection conn, Long id) {
        String sql = "delete from Equipo where id=?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setLong(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
