package cesur.dam.g1;

import cesur.dam.g1.data.DataBaseInitializer;
import cesur.dam.g1.data.DataSourceFactory;
import cesur.dam.g1.data.EquipoJdbcDAO;
import cesur.dam.g1.data.EquipoListDAO;
import cesur.dam.g1.models.Equipo;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.util.Optional;

public class Main2 {


    public static void main(String[] args)
    {

       DataBaseInitializer.initialize(DataSourceFactory.getDataSource());

       var dao = new EquipoJdbcDAO(DataSourceFactory.getDataSource());
       dao.findAll().forEach(System.out::println);

    }
}
