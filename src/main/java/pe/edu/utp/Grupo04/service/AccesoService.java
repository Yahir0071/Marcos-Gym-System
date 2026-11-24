package pe.edu.utp.Grupo04.service;

import java.util.Map;

public interface AccesoService {

    Map<String, Object> generarPaseQr(String username);

    Map<String, Object> validarAcceso(String username, String qrToken);
}
