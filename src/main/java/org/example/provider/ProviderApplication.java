package org.example.provider;

import static spark.Spark.*;

/**
 * Pequeña aplicación HTTP (provider) usando Spark. Está intencionalmente mínima
 * para ilustrar el flujo de contract testing:
 * - Exponemos GET /user/:id que devuelve JSON para el usuario 1.
 * - El test de proveedor arrancará este servidor y pact-jvm verificará las interacciones.
 */
public class ProviderApplication {
    public static void main(String[] args) {
        // Puerto fijo 8080 (el test de verificación usa este puerto)
        port(8080);

        // Ruta simple que devuelve JSON. Para id == "1" devolvemos un usuario válido,
        // para otros ids devolvemos 404.
        get("/user/:id", (req, res) -> {
            res.type("application/json");
            String id = req.params(":id");
            if ("1".equals(id)) {
                return "{\"id\":\"1\",\"name\":\"Alice\"}";
            } else {
                res.status(404);
                return "{\"message\":\"not found\"}";
            }
        });

        // Espera a que arranque el servidor.
        awaitInitialization();
        System.out.println("Provider application started on http://localhost:8080");
    }
}

