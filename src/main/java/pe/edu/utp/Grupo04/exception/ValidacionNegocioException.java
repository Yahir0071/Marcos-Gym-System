package pe.edu.utp.Grupo04.exception;

public class ValidacionNegocioException extends RuntimeException {
    public ValidacionNegocioException(String mensaje) {
        super(mensaje);
    }
}
