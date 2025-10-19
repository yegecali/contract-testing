package org.example.consumer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de consumidor simplificado que genera manualmente un "contract" en JSON
 * y lo escribe en target/pacts. Esto reemplaza la dependencia de la librería Pact
 * para mantener el ejemplo autocontenido.
 *
 * Flujo:
 * 1) La prueba del consumidor crea un fichero JSON que describe las interacciones esperadas.
 * 2) El test de verificación del proveedor (otro test) leerá ese fichero y hará
 *    peticiones HTTP reales contra el proveedor para verificar que cumple el contrato.
 */
public class ConsumerPactTest {

    @Test
    public void generateContractFile() throws IOException {
        String pactJson = "{\n" +
                "  \"consumer\": \"UserConsumer\",\n" +
                "  \"provider\": \"UserProvider\",\n" +
                "  \"interactions\": [\n" +
                "    {\n" +
                "      \"description\": \"get user 1\",\n" +
                "      \"request\": { \"method\": \"GET\", \"path\": \"/user/1\" },\n" +
                "      \"response\": { \"status\": 200, \"body\": { \"id\": \"1\", \"name\": \"Alice\" } }\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        Path pactDir = Path.of("target", "pacts");
        Files.createDirectories(pactDir);
        Path pactFile = pactDir.resolve("UserConsumer-UserProvider.json");
        Files.writeString(pactFile, pactJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Verificamos que el fichero se haya creado y contenga la clave "Alice"
        assertTrue(Files.exists(pactFile), "El fichero de contrato debe existir");
        String read = Files.readString(pactFile);
        assertTrue(read.contains("Alice"), "El contrato debe contener el nombre esperado");
    }
}
