# HelloJdbcG1 — DAO, DataSource y transacciones con JDBC

Repositorio didáctico para aprender, con código Java y MySQL, cómo se organiza
una aplicación sencilla que accede a datos. A lo largo del proyecto se muestran
varias implementaciones del mismo acceso a datos (en memoria y con JDBC), el uso
de `DataSource`, la inicialización de la base de datos, transacciones y un
servicio que orquesta todo.

> Material de clase: contiene varias versiones paralelas y `Main` de ejemplo
> pensados para comparar enfoques. No es un proyecto de producción.

---

## 1. Objetivos

Al terminar de leer y ejecutar este repositorio deberías saber:

- Qué es el patrón DAO y cómo se diseña una interfaz genérica `DAO<T>`.
- Cómo implementar un DAO en memoria con una lista.
- Cómo implementar el mismo contrato con JDBC y MySQL.
- Para qué sirve un `DataSource` y cómo configurarlo con variables de entorno.
- Cómo inicializar el esquema y cargar datos desde scripts SQL.
- Qué es una transacción y qué significa atomicidad.
- Cómo hacer que un DAO participe en una transacción externa.
- Cómo un servicio usa un DAO transaccional.
- Qué problemas de diseño tiene todo esto desde el punto de vista de la
  arquitectura limpia.

---

## 2. Requisitos y puesta en marcha

- **Java 27** (según `pom.xml`).
- **Maven**.
- **MySQL** escuchando en `localhost:3309`, con una base de datos llamada `ad`.
- Dependencias declaradas en `pom.xml`:
  - `com.mysql:mysql-connector-j`
  - `org.projectlombok:lombok` (para `@Data`)

### Variables de entorno

`DataSourceFactory` construye la conexión con la URL:

```text
jdbc:mysql://localhost:3309/ad
```

y lee las credenciales de las variables de entorno **`USER`** y **`PASSWORD`**.
Si no existen, lanza una excepción. En PowerShell:

```powershell
$env:USER = "root"
$env:PASSWORD = "tu_password"
```

> En Windows `USER` suele existir ya con tu nombre de usuario del sistema.
> Sobrescríbela para usar el usuario de MySQL.

### Compilar y ejecutar

```powershell
mvn compile
```

Después ejecuta desde el IDE la clase `Main` que quieras probar.

### Base de datos

La base de datos `ad` debe existir. Los scripts se encargan del resto:

- `init.sql` crea la tabla `Equipo` si no existe.
- `data.sql` inserta 9 equipos de ejemplo.
- `Equipo.sql` es un volcado SQL más grande, útil como copia de la tabla.

---

## 3. Estructura del proyecto

```text
HelloJdbcG1/
├── pom.xml
├── Equipo.sql                    # volcado SQL de la tabla Equipo
├── README.md
└── src/main/
    ├── java/cesur/dam/g1/
    │   ├── Main2.java                       # inicializa BD, carga datos y lista equipos
    │   ├── MainTransaction.java             # transacción con JDBC directo
    │   ├── MainTransactionalDAO.java        # ejemplo con servicio + DAO transaccional
    │   ├── models/
    │   │   └── Equipo.java                  # entidad de dominio (Lombok @Data)
    │   ├── data/
    │   │   ├── DAO.java                     # interfaz genérica de acceso a datos
    │   │   ├── EquipoListDAO.java           # implementación en memoria
    │   │   ├── EquipoJdbcDAO.java           # implementación con JDBC/MySQL
    │   │   ├── DataSourceFactory.java       # fábrica singleton del DataSource
    │   │   ├── DataBaseInitializer.java     # ejecuta init.sql y data.sql
    │   │   ├── TransactionalDAO.java        # contrato con sobrecargas que reciben Connection
    │   │   └── EquipoJdbcTransactionalDAO.java # DAO transaccional para Equipo
    │   └── services/
    │       ├── EquipoService_classic.java       # servicio no transaccional
    │       └── EquipoService_transactional.java # servicio transaccional
    └── resources/
        ├── init.sql                         # DDL de la tabla Equipo
        └── data.sql                         # datos iniciales
```

---

## 4. Diagrama de capas

```text
┌───────────────────────────────────────────────┐
│ Main / ejemplos                               │
│ Main2 · MainTransaction · MainTransactionalDAO│
└──────────────────────┬────────────────────────┘
                       │
┌──────────────────────▼────────────────────────┐
│ Servicios                                     │
│ EquipoService_classic                         │
│ EquipoService_transactional                   │
└──────────────────────┬────────────────────────┘
                       │
┌──────────────────────▼────────────────────────┐
│ Abstracciones DAO (puertos de persistencia)   │
│ DAO<T> · TransactionalDAO<T>                  │
└───────────────┬───────────────────┬───────────┘
                │                   │
     ┌──────────▼─────────┐  ┌──────▼──────────────────────────┐
     │ En memoria          │  │ JDBC                            │
     │ EquipoListDAO       │  │ EquipoJdbcDAO                   │
     │                     │  │ EquipoJdbcTransactionalDAO      │
     └─────────────────────┘  └──────┬──────────────────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │ DataSourceFactory        │
                        │ MysqlDataSource (lazy)   │
                        └────────────┬─────────────┘
                                     │
                        ┌────────────▼─────────────┐
                        │ MySQL localhost:3309/ad  │
                        └──────────────────────────┘
```

Y el ciclo de vida de una transacción:

```text
setAutoCommit(false)
        │
        ▼
   ejecutar acción
        │
   ┌────┴─────┐
   ▼          ▼
commit     rollback
(éxito)    (error)
```

---

## 5. Conceptos explicados paso a paso

### 5.1. El modelo `Equipo`

```java
@Data
public class Equipo {
    private Long id;
    private String nombre;
    private String categoria;
    private Boolean disponible;
}
```

`@Data` de Lombok genera getters, setters, `equals`, `hashCode` y `toString`.
Es un **modelo anémico**: solo guarda datos, no tiene reglas de negocio ni
validaciones.

### 5.2. Patrón DAO genérico

`DAO<T>` define el contrato de acceso a datos para cualquier entidad:

```java
public interface DAO<T> {
    Optional<T> findById(Long id);
    List<T> findAll();
    T save(T obj);
    void update(T entity);
    void deleteById(Long id);
}
```

La idea es separar **qué** se quiere hacer con los datos de **cómo** se hace
(SQL, lista, fichero, etc.). Las capas superiores dependen de la interfaz, no de
la implementación concreta.

### 5.3. Implementación en memoria: `EquipoListDAO`

Guarda los equipos en un `ArrayList`. No necesita base de datos, así que es muy
útil para aprender y para pruebas:

- `findById` / `findAll` usan streams.
- `save` calcula el siguiente id con `max() + 1`.
- `update` busca por id y copia los campos.
- `deleteById` usa `removeIf`.
- Añade `findByNombre` y `findByCategoria`, que no están en `DAO`.

```java
DAO<Equipo> dao = new EquipoListDAO();
Equipo e = new Equipo();
e.setNombre("Proyector");
e.setCategoria("Proyectores");
e.setDisponible(true);
dao.save(e);
System.out.println(dao.findAll());
```

### 5.4. Implementación JDBC: `EquipoJdbcDAO`

Implementa el mismo contrato contra MySQL:

- Usa `PreparedStatement` para evitar inyección SQL.
- `save` usa `Statement.RETURN_GENERATED_KEYS` para recuperar el id.
- El método estático `mapper(ResultSet)` convierte una fila en `Equipo`.
- Cada método abre su propia `Connection` desde el `DataSource` y la cierra con
  try-with-resources.
- Los `SQLException` se envuelven en `RuntimeException`.

### 5.5. `DataSource` y `DataSourceFactory`

Un `DataSource` es una fábrica de conexiones. `DataSourceFactory` crea un
`MysqlDataSource`, le pasa URL, usuario y contraseña, y lo guarda en un
**singleton perezoso** (se crea la primera vez que se pide):

```java
DataSource ds = DataSourceFactory.getDataSource();
```

Ventajas: centraliza la configuración y evita crear conexiones a mano. En este
proyecto la URL está fija en el código y las credenciales vienen de variables de
entorno.

### 5.6. Inicialización de la base de datos

`DataBaseInitializer` ejecuta scripts SQL que están en `src/main/resources`:

1. `initialize(ds)` ejecuta `init.sql`, que crea la tabla `Equipo`.
2. `loadData(ds)` comprueba `select count(*) from Equipo`. **Solo si la tabla
   está vacía** ejecuta `data.sql`.

`data.sql` contiene 9 `INSERT`. El método privado `executeScript` lee el fichero
completo y lo parte por `;`, ejecutando cada sentencia por separado (el driver
MySQL no permite varias sentencias en un solo `execute` por defecto).

### 5.7. Transacciones con JDBC

Una **transacción** agrupa varias operaciones como una unidad:

- `setAutoCommit(false)`: los cambios no se guardan automáticamente.
- `commit()`: se confirman todos.
- `rollback()`: se descartan todos.

La **atomicidad** significa “todo o nada”: si un paso falla, la base de datos
queda como estaba antes de empezar.

Ejemplo en `MainTransaction`:

```java
conn.setAutoCommit(false);
try {
    // ... updates ...
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw new RuntimeException(e);
} finally {
    conn.setAutoCommit(true);
}
```

`MainTransaction` actualiza directamente la tabla (sin DAO) con:

```sql
update Equipo set disponible = false where id = ? and disponible = true
```

Si algún `executeUpdate()` devuelve `0` (el id no existe o ya no estaba
disponible), se hace `rollback()` y no se marca ninguno.

### 5.8. DAO transaccional

`TransactionalDAO<T>` es un contrato independiente de `DAO<T>` (no lo extiende).
Incluye cada operación **dos veces**: la versión normal y una sobrecarga que
recibe una `Connection` externa:

```java
Optional<T> findById(Connection conn, Long id);
List<T> findAll(Connection conn);
T save(Connection conn, T obj);
void update(Connection conn, T entity);
void deleteById(Connection conn, Long id);
```

`EquipoJdbcTransactionalDAO` lo implementa con dos constructores:

- `EquipoJdbcTransactionalDAO(DataSource ds)`: los métodos sin `Connection`
  abren y cierran su propia conexión.
- `EquipoJdbcTransactionalDAO(Connection connection)`: los métodos sin
  `Connection` reutilizan la conexión recibida, que es lo que permite trabajar
  dentro de una transacción.

Regla importante: **los métodos que reciben una `Connection` no la cierran ni
hacen `commit`/`rollback`**. De eso se encarga quien la creó.

### 5.9. Servicios

#### `EquipoService_classic`

Recibe un `DAO<Equipo>` por constructor. Su método `bulkUpdateEquipo`:

1. Valida que todos los ids existan y estén disponibles (primera pasada).
2. Si alguno falla, no hace nada.
3. Si todos son válidos, los actualiza (segunda pasada).

Es **no transaccional**: cada `dao.update` usa su propia conexión, así que si
una actualización fallara a mitad, las anteriores ya estarían guardadas.

#### `EquipoService_transactional`

Recibe un `DataSource` y gestiona la transacción completa. Tiene:

- `executeTransaction(Consumer<Connection>)`: método genérico que abre una
  conexión, desactiva el auto-commit, ejecuta la lambda, hace `commit` si todo
  va bien y `rollback` si algo lanza una excepción.
- `bulkUpdateEquipo(...)`: gestión manual de la transacción.
- `bulkUpdateEquipo2(...)`: la misma lógica, pero delegando en
  `executeTransaction` y usando una excepción (`IllegalStateException`) para
  provocar el rollback.

```java
executeTransaction(conn -> {
    TransactionalDAO<Equipo> dao = new EquipoJdbcTransactionalDAO(conn);
    // operaciones con la misma conexión/transacción
});
```

---

## 6. Ejemplos de uso y salidas esperadas

### `Main2`

Inicializa la tabla, carga los datos (solo si está vacía) y lista todos los
equipos por consola:

```text
Equipo(id=1, nombre=Proyector Epson EB-X06, categoria=Proyectores, disponible=false)
...
```

### `MainTransaction`

Actualiza los ids `[1, 2, 3]` con JDBC directo dentro de una transacción. Si
alguno ya está no disponible, imprime un mensaje y no cambia nada.

### `MainTransactionalDAO`

```java
service.bulkUpdateEquipo(List.of(1L, 2L, 3L));
service.bulkUpdateEquipo2(List.of(4L, 5L, 6L));
service.bulkUpdateEquipo2(List.of(7L, 999L)); // 999 no existe -> rollback
```

La tercera llamada demuestra la atomicidad: aunque el id 7 existe y está
disponible, el id 999 no, así que se revierte todo y el 7 no se modifica.

> Si ejecutas los `Main` varias veces seguidas, los equipos ya estarán a
> `disponible = false`, y los métodos imprimirán “No se actualizó ninguno…”. Es
> el comportamiento esperado.

---

## 7. Problemas desde el punto de vista de arquitectura limpia

Este código es didáctico, pero tiene decisiones que en un proyecto real
convendría revisar:

1. **Lógica de negocio en los `Main`.** `MainTransaction` implementa la
   actualización masiva. La capa de entrada debería limitarse a orquestar, no a
   contener reglas de negocio (SRP / arquitectura por capas).

2. **Dependencias concretas en el servicio.** `EquipoService_transactional`
   hace `new EquipoJdbcTransactionalDAO(conn)` dentro del método. Debería
   depender de una abstracción o de una fábrica inyectada (DIP).

3. **`DataSourceFactory` estático y global.** La URL está fija y las
   credenciales se leen al cargar la clase. Es un singleton difícil de testear
   y de configurar; conviene externalizar la configuración e inyectar el
   `DataSource`.

4. **`TransactionalDAO` no extiende `DAO` y duplica el contrato.** Repite los
   métodos de `DAO` y además mezcla lecturas y escrituras en una sola interfaz
   (ISP).

5. **Sobrecargas con `Connection` y constructores con campos a `null`.**
   `EquipoJdbcTransactionalDAO` deja `ds` o `connection` a `null` según el
   constructor. La propiedad/cierre de la conexión queda implícita y es fácil
   olvidar cerrarla o hacer `commit` en la capa equivocada.

6. **Acoplamiento a JDBC en capas altas.** El servicio importa
   `java.sql.Connection`. Los detalles de infraestructura se filtran hacia
   arriba y contaminan el diseño. La solución es crear un **`TransactionManager`** a
   nivel de la capa de datos que lo encapsule todo.

7. **Modelo anémico.** `Equipo` no valida nada; la regla “marcar como no
   disponible” vive en los servicios. Los objetos de dominio deberían proteger
   sus invariantes.

8. **Salida por consola desde el servicio.** `System.out.println` mezcla
   presentación con lógica de negocio. Convendría devolver un resultado o usar
   logging.

9. **Nombres y duplicación.** `Main2`, `MainTransaction`,
   `MainTransactionalDAO`, `EquipoService_classic`,
   `EquipoService_transactional`, `bulkUpdateEquipo`/`bulkUpdateEquipo2`… Hay
   mucho código repetido y nombres poco convencionales (`snake_case`).

10. **Inicialización de BD mezclada con la aplicación.** `DataBaseInitializer`
    ejecuta DDL y datos desde código. Faltan migraciones versionadas
    (Flyway/Liquibase) y el `count(*) == 0` no es un criterio robusto.

11. **`EquipoListDAO.findAll` devuelve la lista interna.** El llamador puede
    modificarla. Debería devolver una copia o una vista inmutable. Además
    `findByNombre`/`findByCategoria` están fuera de la interfaz.

12. **Sin tests automatizados.** El código no incluye pruebas. La
    implementación en memoria ayudaría a testear sin base de datos, pero no hay
    ejemplos de uso en tests.

13. **Paquetes por capa técnica.** `models`, `data`, `services` separan por
    tipo de clase. En proyectos grandes suele funcionar mejor organizar por
    funcionalidad (feature).

---

## 8. Propuestas de mejora

- Inyectar dependencias por constructor y depender de interfaces (puertos).
- Centralizar las transacciones en un `TransactionManager` o `Unit of Work`.
- Extraer casos de uso que no conozcan `Connection`.
- Añadir validaciones y reglas al modelo o a objetos de valor.
- Externalizar la configuración y usar migraciones versionadas.
- crear un **`TransactionManager`** para gestionar las transacciones.
- Sustituir `System.out` por logging y resultados de operación.
- Añadir tests usando `EquipoListDAO` y dobles de prueba.
- Unificar implementaciones y eliminar código duplicado de los `Main`.

---

## 9. Glosario

- **DAO (Data Access Object):** objeto que encapsula el acceso a una fuente de
  datos.
- **DataSource:** fábrica de conexiones configurada.
- **Transacción:** unidad de trabajo que se confirma o se descarta completa.
- **Atomicidad:** propiedad “todo o nada” de una transacción.
- **commit:** confirma los cambios de la transacción.
- **rollback:** deshace los cambios de la transacción.
- **PreparedStatement:** sentencia SQL precompilada y parametrizada.
- **Migración:** cambio versionado del esquema de base de datos.
- **Inyección de dependencias (DI):** recibir las dependencias desde fuera en
  lugar de crearlas dentro.
- **SRP:** Single Responsibility Principle (una clase, una responsabilidad).
- **DIP:** Dependency Inversion Principle (depender de abstracciones).
- **ISP:** Interface Segregation Principle (interfaces pequeñas y específicas).
