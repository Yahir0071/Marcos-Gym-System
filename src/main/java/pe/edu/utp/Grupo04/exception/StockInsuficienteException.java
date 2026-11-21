package pe.edu.utp.Grupo04.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String producto, int disponible) {
        super("Stock insuficiente para '" + producto + "'. Disponible: " + disponible);
    }
}