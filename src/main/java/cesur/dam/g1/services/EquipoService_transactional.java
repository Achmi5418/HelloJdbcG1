package cesur.dam.g1.services;

import cesur.dam.g1.data.EquipoJdbcTransactionalDAO;
import cesur.dam.g1.data.TransactionalDAO;
import cesur.dam.g1.models.Equipo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Servicio que ejecuta la actualización masiva de equipos dentro de una
 * transacción.
 * <p>
 * Usa {@link EquipoJdbcTransactionalDAO} pasándole la misma {@link Connection}
 * a todas las operaciones, de modo que el servicio es quien confirma
 * ({@code commit}) o revierte ({@code rollback}) la transacción completa.
 * </p>
 */
public class EquipoService_transactional {

    /** Origen de datos del que se obtiene la conexión transaccional. */
    private final DataSource ds;

    /**
     * Crea el servicio.
     *
     * @param ds origen de datos para obtener las conexiones
     */
    public EquipoService_transactional(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Ejecuta una acción dentro de una transacción.
     * <p>
     * Abre una conexión, desactiva el auto-commit, ejecuta la acción recibida y
     * confirma si termina correctamente. Si la acción lanza una excepción, se
     * revierte todo. La conexión se cierra y se restaura el auto-commit en todos
     * los casos.
     * </p>
     *
     * @param action código a ejecutar con la conexión de la transacción
     */
    public void executeTransaction(Consumer<Connection> action) {
        try (Connection conn = ds.getConnection()) {
            // Inicia la transacción: los cambios no se guardan hasta el commit.
            conn.setAutoCommit(false);
            try {
                // Ejecuta el código del llamador con la conexión transaccional.
                action.accept(conn);

                // Si la acción termina bien, se confirman todos los cambios.
                conn.commit();
            } catch (RuntimeException e) {
                // Atomicidad: cualquier error revierte toda la transacción.
                conn.rollback();
                throw e;
            } finally {
                // Restaura el modo normal de la conexión antes de cerrarla.
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Marca como no disponible todos los equipos de los ids de la lista.
     * Si alguno no existe o ya no está disponible, no se marca ninguno.
     * <p>
     * Toda la operación se ejecuta en una única transacción: se recorre la
     * lista una sola vez y, en cuanto un equipo no se puede actualizar, se
     * revierte todo con {@code rollback()}.
     * </p>
     *
     * @param ids identificadores de los equipos a marcar como no disponibles
     */
    public void bulkUpdateEquipo(List<Long> ids) {
        if (ids == null || ids.isEmpty() ) {
            System.out.println("No se actualizó ninguno. Lista de ids vacía o con nulos.");
            return;
        }

        try (Connection conn = ds.getConnection()) {
            // Inicia la transacción: los cambios no se guardan hasta el commit.
            conn.setAutoCommit(false);
            try {
                TransactionalDAO<Equipo> dao = new EquipoJdbcTransactionalDAO(conn);

                for (Long id : ids) {
                    var opt = dao.findById(conn, id);
                    if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getDisponible())) {
                        // Atomicidad: si un id no existe o ya no está disponible,
                        // se revierte la transacción completa y no se marca ninguno.
                        conn.rollback();
                        System.out.println("No se actualizó ninguno. Id inexistente o no disponible: " + id);
                        return;
                    }

                    Equipo eq = opt.get();
                    eq.setDisponible(false);
                    dao.update(conn, eq);
                }

                // Confirma la transacción: todos los cambios se guardan de forma definitiva.
                conn.commit();
                System.out.println("Actualización completada para ids " + ids);
            } catch (SQLException e) {
                // Si algo falla, se revierte todo para mantener la atomicidad.
                conn.rollback();
                throw new RuntimeException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Igual que {@link #bulkUpdateEquipo(List)} pero delegando la gestión de la
     * transacción en {@link #executeTransaction(Consumer)}.
     * <p>
     * Si algún id no existe o ya no está disponible, lanza una excepción para
     * que {@code executeTransaction} haga rollback y no se marque ninguno.
     * </p>
     *
     * @param ids identificadores de los equipos a marcar como no disponibles
     */
    public void bulkUpdateEquipo2(List<Long> ids) {
        if (ids == null || ids.isEmpty() ) {
            System.out.println("No se actualizó ninguno. Lista de ids vacía o con nulos.");
            return;
        }

        try {
            // La transacción (commit/rollback) la gestiona executeTransaction.
            executeTransaction(conn -> {
                TransactionalDAO<Equipo> dao = new EquipoJdbcTransactionalDAO(conn);

                for (Long id : ids) {
                    var opt = dao.findById(conn, id);
                    if (opt.isEmpty() || !Boolean.TRUE.equals(opt.get().getDisponible())) {
                        // La excepción provoca el rollback de toda la transacción.
                        throw new IllegalStateException("Id inexistente o no disponible: " + id);
                    }

                    Equipo eq = opt.get();
                    eq.setDisponible(false);
                    dao.update(conn, eq);
                }
            });

            System.out.println("Actualización completada para ids " + ids);
        } catch (IllegalStateException e) {
            // executeTransaction ya ha hecho rollback al propagar la excepción.
            System.out.println("No se actualizó ninguno. " + e.getMessage());
        }
    }
}
