# Contract Testing — Ejemplo sencillo (Java + Maven)

Este repositorio contiene un ejemplo mínimo que ilustra el paradigma de contract testing (estilo "consumer-driven contract").

Visión rápida
- El test del consumidor genera un fichero JSON que describe el contrato (en `target/pacts`).
- El test de verificación del proveedor arranca la aplicación provider y lee ese fichero para ejecutar peticiones reales y comprobar que el proveedor cumple el contrato.

Requisitos
- Java 17 (o superior compatible)
- Maven (3.x)

Estructura importante
- `pom.xml` — dependencias y configuración Maven.
- `src/main/java/org/example/provider/ProviderApplication.java` — proveedor (servidor HTTP sencillo usando Spark).
- `src/test/java/org/example/consumer/ConsumerPactTest.java` — test del consumidor que genera `target/pacts/UserConsumer-UserProvider.json`.
- `src/test/java/org/example/provider/ProviderPactVerificationTest.java` — test que arranca el proveedor, lee el JSON y verifica las interacciones con peticiones HTTP reales.
- `target/pacts/UserConsumer-UserProvider.json` — fichero generado por el test consumidor (si ejecutaste los tests).

Cómo ejecutar

1) Ejecutar todos los tests (genera el contrato y lo verifica):

```bash
cd /home/genderson/Project/contract-testing
mvn -U test
```

2) Ejecutar solo el test del consumidor (solo genera el JSON del contrato):

```bash
mvn -Dtest=org.example.consumer.ConsumerPactTest test
# después: cat target/pacts/UserConsumer-UserProvider.json
```

3) Ejecutar solo la verificación del proveedor (requiere que exista el fichero en target/pacts):

```bash
mvn -Dtest=org.example.provider.ProviderPactVerificationTest test
```

4) Ejecutar el proveedor manualmente (para probar con curl):

```bash
mvn -DskipTests package dependency:copy-dependencies
java -cp target/classes:target/dependency/* org.example.provider.ProviderApplication
# luego en otra terminal:
# curl http://localhost:8080/user/1
```

Notas y recomendaciones
- El ejemplo que se incluye aquí implementa un flujo "manual" de contract testing para mantener la demostración autocontenida (sin usar las bibliotecas pact-jvm). Los pasos siguen el mismo paradigma: consumidor define expectativas → contrato (JSON) → proveedor verifica.
- Si quieres integrar la librería oficial Pact (pact-jvm) para generar/verificar pacts automáticamente, puedo hacerlo: revisaré versiones y dependencias y lo integro.
- Verás en la salida de Maven una advertencia de SLF4J: "Failed to load class org.slf4j.impl.StaticLoggerBinder". Es solo una advertencia porque no añadimos una implementación de logging; puedes añadir `logback-classic` o `slf4j-simple` para tener logs.
- Las dependencias usadas (Spark, OkHttp, Jackson) son suficientes para este ejemplo. Para usarlo en producción revisa las alertas de seguridad y versiones de dependencias.

Flujo didáctico resumido
1. `ConsumerPactTest` crea `target/pacts/UserConsumer-UserProvider.json` con la interacción esperada (GET /user/1 → 200 con body {id: "1", name: "Alice"}).
2. `ProviderPactVerificationTest` arranca el provider y por cada interacción del JSON hace la petición real y verifica status + campos esperados del body.

Siguientes pasos (opcional)
- Integrar pact-jvm y Pact Broker para un flujo completo CI/CD.
- Añadir más interacciones (headers, POST, validaciones por tipo en lugar de valores concretos).

Si quieres, puedo: integrar Pact real, agregar ejemplos adicionales (POST/headers), o añadir un pequeño script de CI que publique/verifique pacts. ¿Qué prefieres?

