package cesur.dam.g1;

import cesur.dam.g1.data.DataSourceFactory;
import cesur.dam.g1.services.EquipoService_transactional;

import java.util.List;

/**
 * Ejemplo de uso de {@link EquipoService_transactional}, que a su vez utiliza
 * {@link cesur.dam.g1.data.EquipoJdbcTransactionalDAO} para ejecutar la
 * actualización masiva dentro de una transacción.
 */
public class MainTransactionalDAO {

    /**
     * Punto de entrada de ejemplo.
     *
     * @param args argumentos de línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        var service = new EquipoService_transactional(DataSourceFactory.getDataSource());

        // Caso original: transacción gestionada manualmente dentro del servicio.
        System.out.println("== bulkUpdateEquipo ==");
        service.bulkUpdateEquipo(List.of(1L, 2L, 3L));

        // Caso correcto usando executeTransaction: todos los ids existen y
        // están disponibles, por lo que se confirma la transacción.
        System.out.println("== bulkUpdateEquipo2 (caso correcto) ==");
        service.bulkUpdateEquipo2(List.of(4L, 5L, 6L));

        // Caso de error: el id 999 no existe,
        // executeTransaction revierte todo, por lo que el id 7 no se modifica.
        System.out.println("== bulkUpdateEquipo2 (caso con id inexistente -> rollback) ==");
        service.bulkUpdateEquipo2(List.of(7L, 999L));
    }
}
