package cesur.dam.g1.data;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Contrato para operaciones de lectura y escritura que pueden participar en
 * una transacción gestionada por el llamador.
 * <p>
 * Cada operación dispone de dos versiones: una que no recibe conexión y otra
 * sobrecargada que recibe una {@link Connection} externa. Las sobrecargas no
 * abren ni cierran la conexión y tampoco ejecutan {@code commit} ni
 * {@code rollback}: todo eso es responsabilidad de quien invoca.
 * </p>
 *
 * @param <T> tipo de entidad gestionada
 */
public interface TransactionalDAO<T> {

    /**
     * Busca una entidad por su identificador usando una conexión propia.
     *
     * @param id identificador de la entidad
     * @return un {@link Optional} con la entidad encontrada, o vacío si no existe
     */
    Optional<T> findById(Long id);

    /**
     * Busca una entidad por su identificador dentro de una conexión externa.
     *
     * @param conn conexión gestionada por el llamador
     * @param id   identificador de la entidad
     * @return un {@link Optional} con la entidad encontrada, o vacío si no existe
     */
    Optional<T> findById(Connection conn, Long id);

    /**
     * Recupera todas las entidades usando una conexión propia.
     *
     * @return lista con todas las entidades
     */
    List<T> findAll();

    /**
     * Recupera todas las entidades dentro de una conexión externa.
     *
     * @param conn conexión gestionada por el llamador
     * @return lista con todas las entidades
     */
    List<T> findAll(Connection conn);

    /**
     * Guarda una nueva entidad usando una conexión propia.
     *
     * @param obj entidad a guardar
     * @return la entidad guardada con su identificador actualizado
     */
    T save(T obj);

    /**
     * Guarda una nueva entidad dentro de una conexión externa.
     *
     * @param conn conexión gestionada por el llamador
     * @param obj  entidad a guardar
     * @return la entidad guardada con su identificador actualizado
     */
    T save(Connection conn, T obj);

    /**
     * Actualiza una entidad usando una conexión propia.
     *
     * @param entity entidad con los datos actualizados
     */
    void update(T entity);

    /**
     * Actualiza una entidad dentro de una conexión externa.
     *
     * @param conn   conexión gestionada por el llamador
     * @param entity entidad con los datos actualizados
     */
    void update(Connection conn, T entity);

    /**
     * Elimina una entidad por su identificador usando una conexión propia.
     *
     * @param id identificador de la entidad a eliminar
     */
    void deleteById(Long id);

    /**
     * Elimina una entidad por su identificador dentro de una conexión externa.
     *
     * @param conn conexión gestionada por el llamador
     * @param id   identificador de la entidad a eliminar
     */
    void deleteById(Connection conn, Long id);
}
