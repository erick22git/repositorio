// 1) En IPlataforma.java (la de la plataforma Y la copia del comercio) agregar:
String consultarSaldo(String nrocuenta) throws RemoteException;

// 2) En Plataforma.java agregar este metodo (reutiliza pedir, campo y BANCOS):
@Override
public String consultarSaldo(String nrocuenta) throws RemoteException {
    try {
        String login = pedir("POST", ATC, "{\"user\":\"comercio\",\"password\":\"1234\"}", null).body();
        String token = campo(login, "token");
        for (String banco : BANCOS) {
            HttpResponse<String> r = pedir("GET", banco + "/" + nrocuenta, null, token);
            if (r.statusCode() == 200) return r.body();
        }
        return "cuenta no encontrada";
    } catch (Exception e) {
        return "error: " + e.getMessage();
    }
}

// 3) En SitioComercio.java, donde quieras consultar:
System.out.println(plataforma.consultarSaldo(origen));
