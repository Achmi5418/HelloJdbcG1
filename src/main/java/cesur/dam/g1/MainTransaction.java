package cesur.dam.g1;

import cesur.dam.g1.data.DataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Ejemplo de uso de transacciones con JDBC.
 * <p>
 * Una <b>transacción</b> agrupa varias operaciones sobre la base de datos como
 * una única unidad de trabajo: se confirman todas juntas con {@code commit()} o
 * se descartan todas con {@code rollback()}.
 * </p>
 * <p>
 * La <b>atomicidad</b> es la propiedad que garantiza que la transacción se
 * aplica "todo o nada". Si cualquier paso falla, se deshace lo hecho hasta ese
 * momento y la base de datos queda como estaba antes de empezar.
 * </p>
 */
public class MainTransaction {

    public static void main(String[] args) {
        List<Long> ids = List.of(1L, 2L, 3L);
        bulkUpdateEquipo(DataSourceFactory.getDataSource(), ids);
    }

    public static void bulkUpdateEquipo(DataSource ds, List<Long> ids) {
        if (ids == null || ids.isEmpty() ){
            System.out.println("No se actualizó ninguno. Lista de ids vacía.");
            return;
        }

        try (Connection conn = ds.getConnection()) {
            // Inicia la transacción: los cambios no se guardan hasta el commit.
            conn.setAutoCommit(false);
            try {
                // Se actualiza directamente: si alguna sentencia no actualiza filas,
                // se revierte todo.
                String updateSql = "update Equipo set disponible = false where id = ? and disponible = true";
                try (PreparedStatement pst = conn.prepareStatement(updateSql)) {
                    for (Long id : ids) {
                        pst.setLong(1, id);
                        if (pst.executeUpdate() == 0) {
                            // Atomicidad: la transacción se revierte completa y no se marca ninguno.
                            conn.rollback();
                            System.out.println("No se actualizó ninguno. Id no disponible: " + id);
                            return;
                        }
                    }
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
}
