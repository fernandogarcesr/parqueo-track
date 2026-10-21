# FASE 4 — Pruebas AAA y `assertThrows`

**Qué vas a lograr:** escribir pruebas unitarias claras, detectar defectos de lógica de negocio con ellas, diagnosticar cada rojo y corregir el código (no la prueba).
**Curso:** Semana 5 · P1 "AAA + assertThrows".

---

## 1. Conceptos

### Qué es una prueba unitaria
Código que ejecuta una **unidad pequeña** de tu programa (una regla) y verifica el resultado. Corre en milisegundos, sin red ni base de datos.
Las pruebas son la **evidencia** de que el software cumple la regla: sin pruebas, "funciona" es una opinión.

### JUnit 5 en 60 segundos
- `@Test` marca un método como prueba. JUnit crea una **instancia nueva de la clase por cada prueba** (por eso una prueba no hereda el estado de otra).
- `assertEquals(esperado, obtenido)`: **primero lo esperado, después lo obtenido** (si los inviertes, el mensaje de error miente).
- `assertTrue / assertFalse`: para condiciones booleanas.
- `assertThrows(Tipo.class, () -> codigo)`: verifica que el código **lanza** esa excepción. El `() -> ...` es una *lambda* (código que se pasa para ejecutarse dentro del assert; así JUnit controla el `try/catch`). Devuelve la excepción por si quieres revisar su mensaje.
- Las pruebas que no lanzan excepción y no fallan un assert **pasan**.

### El patrón AAA
```java
@Test
void nombreQueDiceLaRegla() {
    // Arrange: preparo los datos y los objetos
    // Act: ejecuto UNA acción
    // Assert: verifico el resultado
}
```
Buenas prácticas:
- Nombre = **la regla en palabras** (`quinceMinutosSiguenSiendoGratis`), no `test1`.
- **Una idea por prueba.** Si falla, sabes exactamente qué regla se rompió.
- Prueba contra **la regla acordada**, no contra lo que hace el código hoy. (Compara con la REGLA, no con el código.)
- Pruebas independientes entre sí: cada una crea sus propios objetos.

### Cómo se diagnostica un rojo
Cada fallo de JUnit trae: **qué prueba** falló, **qué se esperaba** y **qué se obtuvo**:
```
PaseDescuentoTest.minusculasSeAceptan:   expected: <20> but was: <IllegalArgumentException ...>
```
Pregúntate: ¿mi prueba refleja bien la regla? Si sí, el **defecto está en el código**. Si no, el defecto está en la prueba. Jamás "arreglas" un rojo cambiando el assert para que pase: eso es manipular la evidencia y la prueba deja de valer.

### Falsos positivos (la prueba que miente)
Una prueba puede pasar aunque el código esté mal. Ejemplo real en este proyecto: `PaseDescuento` compara con `==`. En Java, `==` en objetos compara **la referencia**, no el contenido. Con el literal `"ITSON20"` en una prueba, el compilador reutiliza la MISMA cadena y `==` da `true`... pero con un texto que llega de HTTP (`pase=ITSON20`) ya es otro objeto y falla.
Lección: cubre varias formas de entrada (minúsculas, con espacios, texto "que viene de fuera") y no te fíes de un solo caso feliz.

---

## 2. Pasos

### Paso 1 — Rama
```powershell
git switch main
git pull
git switch -c feature/pruebas-pase-y-cobro
```

### Paso 2 — Escribe `PaseDescuentoTest` (esqueleto)
`parqueo-api/src/test/java/mx/itson/devops/parqueo/PaseDescuentoTest.java`
La regla (léela en el comentario de `PaseDescuento.java`): ITSON20 → 20, EGRESADO10 → 10, null/blanco → 0, no distingue mayúsculas, ignora espacios, cualquier otro → `IllegalArgumentException`.
```java
package mx.itson.devops.parqueo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaseDescuentoTest {

    @Test
    void itson20DaVeintePorCiento() {
        // Arrange
        PaseDescuento pase = new PaseDescuento();
        // Act
        int pct = pase.porcentaje("ITSON20");
        // Assert
        assertEquals(20, pct);
    }

    @Test
    void elCodigoNoDistingueMayusculas() {
        // TODO Arrange: crea el objeto
        // TODO Act: pide el porcentaje de "itson20" (minúsculas)
        // TODO Assert: ¿qué dice la REGLA que debe devolver?
    }

    @Test
    void seIgnoranLosEspaciosAlInicioYAlFinal() {
        // TODO con "  EGRESADO10 "
    }

    @Test
    void sinPaseNoHayDescuento() {
        // TODO cubre null y también una cadena en blanco ("   ")
    }

    @Test
    void unCodigoDesconocidoSeRechaza() {
        PaseDescuento pase = new PaseDescuento();

        // assertThrows: el código dentro de la lambda DEBE lanzar esa excepción
        assertThrows(IllegalArgumentException.class, () -> pase.porcentaje("XYZ"));
    }
}
```
Completa los TODO **con tus palabras y tu criterio**. Luego:
```powershell
mvn -B test -Dtest=PaseDescuentoTest
```
- `-Dtest=Clase` ejecuta solo esa clase (`-D` define una propiedad del sistema que lee Surefire). También puedes apuntar a un método: `-Dtest=PaseDescuentoTest#itson20DaVeintePorCiento`.
- Se esperan **fallos**: mayúsculas y espacios.
- Escribe la tabla de diagnóstico (abajo) **antes** de arreglar.

### Paso 3 — Tabla de diagnóstico (llénala con TUS datos)
| Prueba | Esperado | Obtenido | Causa raíz | Dónde arreglo (código o prueba) |
|---|---|---|---|---|
| | | | | |

### Paso 4 — Commits separados: ROJO y VERDE
```powershell
git add parqueo-api/src/test
git commit -m "test: reglas de PaseDescuento (AAA y assertThrows)"
```
Arregla `PaseDescuento.java` **sin tocar las pruebas**: normaliza el texto (quita espacios con `trim()`, ignora mayúsculas) y compara **contenido** con `.equals()` / `equalsIgnoreCase()`, nunca con `==`.
```powershell
mvn -B test
git add parqueo-api/src/main
git commit -m "fix: PaseDescuento compara contenido, ignora mayusculas y espacios"
```

### Paso 5 — Las demás reglas (mismo ciclo: prueba roja → arreglo → verde)
Haz **una prueba AAA por cada regla** y, para las que fallen, el arreglo en commit aparte:

| Clase | Regla (lee el comentario de la clase) | Qué pruebas escribir | Qué debes encontrar |
|---|---|---|---|
| `CobroService` | total = tarifa × (100 − desc) / 100, entero | 30 min sin pase → 10; 30 min ITSON20 → 8; 61 min ITSON20 → 14; 600 min ITSON20 → 64 | el descuento no se aplica bien (división entera en mal orden) |
| `Estacionamiento` | con capacidad 3 caben exactamente 3 | ingresar 3 autos OK; el 4º → `assertThrows(IllegalStateException.class, ...)`; placa repetida; `salir` de una placa que no está; placa en blanco | el límite permite uno de más |
| `AvisoService` | recibo con placa mayúscula, total 0 → "sin cargo", negativo → excepción | los 3 casos + null | (probablemente nada: así sabes qué se siente un módulo sano) |

Sugerencia para `CobroService`: la forma correcta de aplicar el descuento entero es multiplicar **antes** de dividir (`total * (100 - descuento) / 100`). Analiza por qué `total / 100 * descuento` da 0 para totales menores que 100.

### Paso 6 — Resultado final
```powershell
mvn -B verify
```
Debe terminar `BUILD SUCCESS` con un número de pruebas mayor a 15. Abre el PR (con la tabla y los links del rojo y del verde) y mergea con merge commit.

---

## 3. Evidencia a guardar
- [ ] Tabla de diagnóstico completa (3 fallos como mínimo).
- [ ] Commits separados: `test:` (rojo) y `fix:` (verde) por cada defecto.
- [ ] Salida de `mvn -B verify` en verde con el conteo de pruebas.

## 4. Para entender de verdad
1. ¿Por qué `assertEquals(20, pct)` y no `assertEquals(pct, 20)`?
2. ¿Qué diferencia hay entre `==` y `.equals()` en Java con cadenas? ¿Por qué el literal pasaba y el texto de HTTP no?
3. ¿Qué pasa si "arreglas" un rojo cambiando el valor esperado? ¿Qué dejas de demostrar?
4. ¿Por qué el nombre de la prueba debe describir la regla?
5. ¿Qué es una lambda y por qué `assertThrows` la necesita?
