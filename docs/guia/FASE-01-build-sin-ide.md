# FASE 1 — El build sin IDE (Maven)

**Qué vas a lograr:** compilar, probar y empaquetar desde la terminal; leer un error de build; arreglar un build roto; entender el POM, el ciclo de vida y las dependencias.
**Curso:** Semana 4 · P1 "El build sin IDE".

---

## 1. Conceptos

### Compilar, construir, probar y empaquetar son cosas distintas
| Etapa | Qué es | Dónde queda |
|---|---|---|
| Código fuente | `.java` legible para personas; aquí vive la regla de negocio | `src/main/java` |
| Bytecode | `.class`, lo produce el compilador | `target/classes` |
| Pruebas | verifican COMPORTAMIENTO (el compilador solo verifica sintaxis y tipos) | `target/surefire-reports` |
| JAR | empaqueta clases y recursos; **no es un despliegue** | `target/*.jar` |

> **El error más caro:** decir "ya compila" como si fuera "ya funciona". Compilar valida sintaxis; las pruebas validan comportamiento.

### Maven no adivina: todo es convención
```
parqueo-track/
├── pom.xml                  ← POM padre (identidad del proyecto y reglas comunes)
├── parqueo-api/
│   ├── pom.xml              ← módulo
│   └── src/
│       ├── main/java/...    ← código de producción
│       └── test/java/...    ← pruebas
├── avisos/ (igual)
└── target/ (en cada módulo) ← GENERADO por el build; se borra con mvn clean; NUNCA se versiona
```
- Maven se ejecuta **siempre en la carpeta que contiene un `pom.xml`**. Si no: *"there is no POM in this directory"*.
- **Dos classpaths:** `src/main/java` se compila SIN JUnit; `src/test/java` se compila CON las dependencias de scope `test`.

### El POM declara identidad y reglas, no pasos
- **Coordenadas (GAV):** `groupId` + `artifactId` + `version` = dirección única del artefacto (`mx.itson.devops:parqueo-api:1.0.0-SNAPSHOT`).
- **`<properties>`:** valores reutilizables. `maven.compiler.release=21` fija el Java del bytecode sin depender del IDE.
- **Dependencia vs plugin:** la dependencia es **código de terceros** que entra al classpath (`junit-jupiter`); el plugin es **una tarea** del build (`maven-surefire-plugin`).
- **`scope=test`:** la dependencia solo existe para `src/test`. No se empaqueta.
- **`<parent>` y `<modules>`:** el POM padre agrupa módulos (`packaging=pom`); los módulos heredan versiones. A esto se le llama build **multi-módulo** y Maven los construye en un orden llamado *reactor*.
- **`failIfNoTests=true`:** si no corre NINGUNA prueba, el build falla (por omisión sería "éxito": un verde vacío engaña).

### Ciclo de vida: avanzar a una fase ejecuta TODAS las anteriores
| Comando | Qué ejecuta | Qué produce |
|---|---|---|
| `mvn clean` | borra `target/` (es OTRO ciclo, no una fase del ciclo por omisión) | — |
| `mvn compile` | compila `src/main/java` | `target/classes` |
| `mvn test` | compile + ejecuta pruebas (Surefire) | `target/surefire-reports/` |
| `mvn package` | compile + test + arma el JAR | `target/*.jar` |
| `mvn verify` | hasta package + verificaciones configuradas | — |
| `mvn install` | ... + copia el artefacto a tu `.m2` local | `~/.m2/repository/...` |
| `mvn deploy` | ... + lo publica en un repositorio REMOTO (Nexus). **No despliega en AWS** | en Nexus |

Por eso un `BUILD SUCCESS` de `package` ya significa que las pruebas pasaron.
- **Fase** = una etapa del ciclo (`test`). **Goal** = una tarea concreta de un plugin (`surefire:test`).
- `-B` (*batch*) = sin colores ni barras de progreso: el log queda igual que en CI.

---

## 2. Pasos

### Paso 1 — Reproduce el rojo en tu máquina
```powershell
cd C:\Proyectos\parqueo-track
mvn -B compile
```
Resultado esperado: **BUILD FAILURE**. Lee la salida de abajo hacia arriba:
1. `[ERROR] ...ParqueoSmokeTest.java:[3,..] package org.junit.jupiter.api does not exist` ← la causa.
2. `Reactor Summary` ← qué módulo falló (`ParqueoTrack API ... FAILURE`) y cuáles se saltaron (`SKIPPED`).
3. `[ERROR] Failed to execute goal ...maven-compiler-plugin...:compile` ← qué plugin/tarea falló.

Compáralo con el log del run rojo de GitHub: **es el mismo error**. Eso es reproducibilidad: el CI y tu terminal dicen lo mismo.

### Paso 2 — Diagnostica (¿por qué falla?)
- El archivo `ParqueoSmokeTest.java` usa JUnit, pero está en `src/main/java`.
- `src/main` se compila SIN JUnit (JUnit tiene `scope=test`) → "el paquete no existe".
- Dos "arreglos" posibles:
  - ❌ Quitar `<scope>test</scope>`: compila, pero **JUnit viajaría dentro del JAR de producción**. Es tapar el síntoma.
  - ✅ Mover la clase a `src/test/java`: es una prueba, ahí vive.

### Paso 3 — Arréglalo (commit 1)
```powershell
git switch -c fix/mover-prueba-humo
git mv parqueo-api\src\main\java\mx\itson\devops\parqueo\ParqueoSmokeTest.java parqueo-api\src\test\java\mx\itson\devops\parqueo\ParqueoSmokeTest.java
git status
mvn -B compile
mvn -B test
```
- `git switch -c nombre` crea una rama nueva y te cambia a ella (`-c` = *create*). Trabajar en ramas es la regla desde la Semana 2.
- `git mv origen destino` mueve el archivo **y** lo deja en staging (Git registra un *rename*, conserva el historial).
- `git status` debe mostrar `renamed:`.
- `mvn -B compile` ahora pasa. `mvn -B test` va a **fallar otra vez** (¡otro motivo!).

### Paso 3b — Segundo fallo: `avisos` no tiene pruebas
`mvn -B test` ahora ejecuta 5 pruebas en `parqueo-api` (todas pasan), pero el módulo `avisos` falla con algo como `No tests to run!` o `No tests were executed!`.
- Causa: el padre configura `failIfNoTests=true` y `avisos` no tiene ninguna prueba.
- ❌ No pongas `failIfNoTests=false` (es esconder el problema).
- ✅ Escribe la primera prueba del módulo. Crea `avisos/src/test/java/mx/itson/devops/avisos/AvisoServiceSmokeTest.java`:
```java
package mx.itson.devops.avisos;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AvisoServiceSmokeTest {

    @Test
    void sinCargoCuandoElTotalEsCero() {
        // Arrange: preparo el servicio
        AvisoService servicio = new AvisoService();
        // Act: pido el recibo de un total de 0
        String recibo = servicio.recibo("abc123", 0);
        // Assert: debe decir que no hay cargo
        assertEquals("Placa ABC123: sin cargo", recibo);
    }
}
```
(Esa estructura Arrange / Act / Assert se llama **AAA**; la estudias a fondo en la Fase 4.)

Comprueba que pasa y haz el commit:
```powershell
mvn -B test
```
Ahora debes ver `Tests run: 5` (parqueo-api) y `Tests run: 1` (avisos), y `BUILD SUCCESS`.

### Paso 4 — Dos commits, dos "historias" (así queda claro en el log y en CI)
```powershell
git add parqueo-api
git commit -m "fix: mover ParqueoSmokeTest de src/main a src/test"
git push -u origin fix/mover-prueba-humo
```
Mira Actions: el run de este commit saldrá ROJO por `avisos` (otro rojo, otra causa). Léelo: debe decir lo mismo que viste en tu terminal.
```powershell
git add avisos
git commit -m "test: primera prueba del modulo avisos"
git push
```
Ahora el run sale VERDE. Abre un **Pull Request** hacia `main` y **mergéalo** (en la Fase 3 estudias PR a fondo).

### Paso 5 — Inspecciona qué produce el build
```powershell
mvn -B package
dir parqueo-api\target
jar -tf parqueo-api\target\parqueo-api-1.0.0-SNAPSHOT.jar
```
- `dir` muestra `classes/`, `test-classes/`, `surefire-reports/` y el `.jar`.
- `jar -tf archivo.jar` (*t* = listar, *f* = archivo) enseña el contenido: solo tus clases de `src/main` y `META-INF/MANIFEST.MF`. **Ninguna** clase de JUnit.
- Abre `parqueo-api\target\surefire-reports\` y mira el `.txt` de resultados.

### Paso 6 — Ejecuta el `.jar`
```powershell
java -jar parqueo-api\target\parqueo-api-1.0.0-SNAPSHOT.jar
```
En **otra** terminal:
```powershell
curl.exe http://localhost:8080/health
curl.exe "http://localhost:8080/api/cobro?minutos=30"
```
- `java -jar` ejecuta la clase main que declaró `maven-jar-plugin` en el manifiesto.
- Usa `curl.exe` (con `.exe`): en PowerShell `curl` a secas es un alias de otro comando.
- Debes ver `{"status":"UP"}` y `{"minutos":30,"pase":"","total":10}`. Detén el servidor con `Ctrl + C`.

### Paso 7 — Dependencias
```powershell
mvn -B dependency:tree
```
- Imprime el árbol de dependencias de cada módulo: `junit-jupiter` trae por **transitividad** `junit-jupiter-api`, `junit-jupiter-engine`, `junit-jupiter-params`, `junit-platform-*`, `opentest4j`... Tú declaraste UNA; Maven resolvió varias.
- `dependency:tree` = plugin `dependency`, goal `tree` (se descarga la primera vez).
- Guarda el árbol de `parqueo-api` como evidencia.

### Paso 8 — Limpia
```powershell
mvn -B clean
dir parqueo-api
```
La carpeta `target/` desaparece. Es desechable: por eso va en `.gitignore`.

---

## 3. Evidencia a guardar
- [ ] Captura del `mvn -B compile` roto (error de JUnit) + link al run rojo.
- [ ] Link al segundo run rojo (avisos) y al verde.
- [ ] Salida de `mvn -B test` en verde (`Tests run: ...`).
- [ ] Salida de `jar -tf` y de `mvn dependency:tree`.

## 4. Errores comunes
| Síntoma | Causa | Arreglo |
|---|---|---|
| `there is no POM in this directory` | estás en otra carpeta | `cd` a la que tiene `pom.xml` |
| `release version 21 not supported` | Maven usa un JDK menor | `JAVA_HOME` apuntando al JDK 21 |
| `Could not resolve dependencies` | sin internet / dependencia mal escrita | revisa red y coordenadas |
| `'mvn' no se reconoce` | falta Maven en el PATH | agrega `...\apache-maven\bin` al PATH |

## 5. Para entender de verdad
1. ¿Por qué mover el archivo es mejor que quitar el `scope=test`?
2. ¿Por qué `mvn package` "incluye" `test`?
3. ¿Qué diferencia hay entre `install` y `deploy`?
4. ¿Por qué un JAR que alguien manda por chat "no es evidencia"?
