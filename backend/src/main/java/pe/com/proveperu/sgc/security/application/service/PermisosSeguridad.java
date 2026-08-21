package pe.com.proveperu.sgc.security.application.service;

import java.util.Set;

public final class PermisosSeguridad {

    public static final String USUARIOS_VER = "SEG_USUARIOS_VER";
    public static final String USUARIOS_CREAR = "SEG_USUARIOS_CREAR";
    public static final String USUARIOS_EDITAR = "SEG_USUARIOS_EDITAR";
    public static final String USUARIOS_ESTADO = "SEG_USUARIOS_ESTADO";
    public static final String USUARIOS_PASSWORD = "SEG_USUARIOS_PASSWORD";
    public static final String ROLES_VER = "SEG_ROLES_VER";
    public static final String ROLES_CREAR = "SEG_ROLES_CREAR";
    public static final String ROLES_PERMISOS = "SEG_ROLES_PERMISOS";
    public static final String PERMISOS_VER = "SEG_PERMISOS_VER";

    public static final Set<String> ADMINISTRADOR_OBLIGATORIOS = Set.of(
        USUARIOS_VER,
        USUARIOS_CREAR,
        USUARIOS_EDITAR,
        USUARIOS_ESTADO,
        USUARIOS_PASSWORD,
        ROLES_VER,
        ROLES_CREAR,
        ROLES_PERMISOS,
        PERMISOS_VER
    );

    private PermisosSeguridad() {
    }
}
