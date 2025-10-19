package org.example.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación manual del contrato (sin usar librerías Pact).
 *
 * Flujo:
 * 1) Arranca el proveedor (ProviderApplication) en @BeforeAll
 * 2) Lee el fichero de contrato JSON generado por el test de consumidor en target/pacts
 * 3) Para cada interacción definida, realiza la petición HTTP real contra el proveedor
 *    y verifica status y contenido del body (comparación parcial del JSON esperado)
 * 4) Para este ejemplo simplificado comprobamos que el proveedor cumple el contrato.
 */
public class ProviderPactVerificationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final OkHttpClient CLIENT = new OkHttpClient();

    @BeforeAll
    static void startProvider() throws Exception {
        // Llamamos al main de la aplicación provider para arrancar Spark en el mismo JVM
        // ProviderApplication usa awaitInitialization() así que cuando retorna el servidor estará listo.
        Thread t = new Thread(() -> ProviderApplication.main(new String[]{}));
        t.setDaemon(true);
        t.start();
        // small sleep to be resilient en caso de que la app tarde en arrancar (awaitInitialization ya espera, pero reforzamos)
        Thread.sleep(500);
        System.out.println("Provider started for manual pact verification on http://localhost:8080");
    }

    @AfterAll
    static void stopProvider() throws Exception {
        // Detenemos Spark
        spark.Spark.stop();
        spark.Spark.awaitStop();
        System.out.println("Provider stopped");
    }

    @Test
    void verifyContractGeneratedByConsumer() throws Exception {
        Path pactFile = Path.of("target", "pacts", "UserConsumer-UserProvider.json");
        assertTrue(Files.exists(pactFile), "El fichero de contrato debe existir (ejecuta antes el test del consumidor)");

        String json = Files.readString(pactFile);
        JsonNode root = MAPPER.readTree(json);
        JsonNode interactions = root.get("interactions");
        assertTrue(interactions != null && interactions.isArray() && interactions.size() > 0, "Debe haber al menos una interacción en el contrato");

        for (JsonNode inter : interactions) {
            JsonNode request = inter.get("request");
            JsonNode response = inter.get("response");
            String method = request.get("method").asText();
            String path = request.get("path").asText();
            int expectedStatus = response.get("status").asInt();
            JsonNode expectedBody = response.get("body");

            // Hacemos la petición HTTP real contra el proveedor
            Request req = new Request.Builder()
                    .url("http://localhost:8080" + path)
                    .method(method, null)
                    .build();

            try (Response resp = CLIENT.newCall(req).execute()) {
                int actualStatus = resp.code();
                assertEquals(expectedStatus, actualStatus, "El status devuelto por el provider no coincide con el contrato");
                String bodyStr = resp.body() != null ? resp.body().string() : "";

                if (expectedBody != null && !expectedBody.isNull()) {
                    // Comparamos parcialmente: comprobamos que todos los campos del expectedBody
                    // están presentes y con los mismos valores en la respuesta real.
                    JsonNode actualBodyNode = null;
                    try {
                        actualBodyNode = MAPPER.readTree(bodyStr);
                    } catch (IOException e) {
                        throw new AssertionError("La respuesta del provider no es JSON válido: " + bodyStr);
                    }

                    assertTrue(jsonContains(actualBodyNode, expectedBody), "La respuesta JSON no cumple el contrato esperado.\nEsperado: " + expectedBody + "\nReal: " + actualBodyNode);
                }
            }
        }
    }

    // Helper: comprueba recursivamente que 'actual' contiene los campos de 'expected' y con los mismos valores
    private static boolean jsonContains(JsonNode actual, JsonNode expected) {
        if (expected.isObject()) {
            if (!actual.isObject()) return false;
            for (String fieldName : iterable(expected.fieldNames())) {
                JsonNode expectedVal = expected.get(fieldName);
                JsonNode actualVal = actual.get(fieldName);
                if (actualVal == null) return false;
                if (!jsonContains(actualVal, expectedVal)) return false;
            }
            return true;
        } else if (expected.isArray()) {
            if (!actual.isArray()) return false;
            // para arrays, comprobamos que cada elemento esperado esté contenido en el array real
            for (JsonNode expElem : expected) {
                boolean found = false;
                for (JsonNode actElem : actual) {
                    if (jsonContains(actElem, expElem)) { found = true; break; }
                }
                if (!found) return false;
            }
            return true;
        } else {
            // valores escalares: comparamos como texto
            return actual.asText().equals(expected.asText());
        }
    }

    // Convierte un Iterator a Iterable para usar en for-each
    private static <T> Iterable<T> iterable(java.util.Iterator<T> it) {
        return () -> it;
    }
}
