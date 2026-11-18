package pe.edu.utp.Grupo04.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String tipo = "Bearer";
    private String username;
    private String rol;
    private long expiracionMs;

    public AuthResponseDTO(String token, String username, String rol, long expiracionMs) {
        this.token = token;
        this.tipo = "Bearer";
        this.username = username;
        this.rol = rol;
        this.expiracionMs = expiracionMs;
    }
}
