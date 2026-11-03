package cesur.dam.g1.services;

import cesur.dam.g1.data.DAO;
import cesur.dam.g1.models.Equipo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EquipoService_classic {

    private final DAO<Equipo> dao;

    public EquipoService_classic(DAO<Equipo> dao) {
        this.dao = dao;
    }

    /**
     * Marca como no disponible todos los equipos
     * de los ids de la lista.
     * Si alguno no esta disponible, no se deberá marcar ninguno.
     * <p>
     * Implementación no transaccional: no es atómica. Primero se validan todos
     * los equipos y después se actualizan, pero cada actualización usa su propia
     * conexión, por lo que si una falla a mitad, las anteriores ya quedan
     * persistidas.
     * </p>
     *
     * @param ids identificadores de los equipos a marcar como no disponibles
     */
    public void bulkUpdateEquipo(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        List<Equipo> equipos = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) return;

            Optional<Equipo> opt = dao.findById(id);
            if (opt.isEmpty()) return;

            Equipo eq = opt.get();
            if (!Boolean.TRUE.equals(eq.getDisponible())) return;

            equipos.add(eq);
        }

        for (Equipo eq : equipos) {
            eq.setDisponible(false);
            dao.update(eq);
        }
    }
}
