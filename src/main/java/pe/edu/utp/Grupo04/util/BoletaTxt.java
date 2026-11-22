package pe.edu.utp.Grupo04.util;

import java.util.Map;

/** Arma el texto del comprobante simulado (descarga .txt). */
public final class BoletaTxt {

    private BoletaTxt() {}

    public static String construir(Map<String, Object> o) {
        String doble = "=".repeat(72);
        String simple = "-".repeat(72);
        StringBuilder sb = new StringBuilder();
        sb.append(doble).append('\n')
          .append("             ENERGY GYM - COMPROBANTE DE PAGO SIMULADO\n")
          .append("          DOCUMENTO INTERNO SIN VALIDEZ TRIBUTARIA\n")
          .append(doble).append('\n')
          .append("Comprobante        : ").append(o.get("boleta")).append('\n')
          .append("Código de Pedido   : ").append(o.get("codigo")).append('\n')
          .append("N° de Operación    : ").append(o.get("operacion")).append('\n')
          .append("Fecha              : ").append(o.get("fecha")).append('\n')
          .append("Estado de Pago     : ").append(o.get("estadoPago")).append('\n')
          .append("Estado de Entrega  : ").append(o.get("estadoEntrega")).append('\n')
          .append(simple).append('\n')
          .append("DATOS DEL CLIENTE\n")
          .append("Cliente            : ").append(o.get("buyerName")).append('\n')
          .append("Documento          : ").append(o.get("buyerDocType")).append(' ').append(o.get("buyerDocNum")).append('\n')
          .append("Correo             : ").append(o.get("buyerEmail")).append('\n')
          .append("Método             : Pago simulado\n")
          .append(simple).append('\n')
          .append("DETALLE\n")
          .append(o.get("resumenItems")).append('\n')
          .append(simple).append('\n')
          .append(String.format("Base               : S/ %s%n", o.get("baseFormatted")))
          .append(String.format("IGV (18%%)          : S/ %s%n", o.get("igvFormatted")))
          .append(String.format("IMPORTE TOTAL      : S/ %s%n", o.get("totalFormatted")))
          .append(doble).append('\n');
        if ("PRODUCTO".equals(o.get("tipoConcepto"))) {
            sb.append("Lugar de recojo    : Recepción de Energy Gym (sede Las Américas, Ate)\n");
        }
        sb.append("Aviso              : Documento interno sin validez tributaria.\n")
          .append(doble).append('\n');
        return sb.toString();
    }
}
