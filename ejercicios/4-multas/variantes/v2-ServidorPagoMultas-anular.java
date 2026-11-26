// En ServidorPagoMultas.java REEMPLAZAR el metodo procesarSolicitud por este.
// PAGAR:codigo:ci   ANULAR:codigo:ci   ESTADO:codigo:ci
static synchronized String procesarSolicitud(String mensaje) {
    try {
        String[] p = mensaje.split(":");
        if (p.length != 3) return "ERROR:use OPERACION:codigo:ci";
        String url = REST + URLEncoder.encode(p[1], "UTF-8");

        HttpResponse<String> r = pedir("GET", url, null);
        if (r.statusCode() != 200) return "ERROR:la multa no existe";
        if (!p[2].equals(campo(r.body(), "ci"))) return "ERROR:la multa no es de ese conductor";
        String estado = campo(r.body(), "estado");

        if (p[0].equals("ESTADO")) return "OK:" + estado;

        String nuevo = p[0].equals("PAGAR") ? "pagada" : p[0].equals("ANULAR") ? "anulada" : null;
        if (nuevo == null) return "ERROR:operacion desconocida";
        if (!"pendiente".equals(estado)) return "ERROR:la multa esta " + estado;

        r = pedir("PATCH", url, "{\"estado\":\"" + nuevo + "\"}");
        return r.statusCode() == 200 ? "OK:multa " + nuevo : "ERROR:no se pudo registrar";

    } catch (Exception e) {
        return "ERROR:la central REST no responde";
    }
}
