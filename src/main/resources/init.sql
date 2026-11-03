create table if not exists Equipo
(
    id         int auto_increment
        primary key,
    nombre     varchar(80) not null,
    categoria  varchar(80) null,
    disponible tinyint(1)  null
);