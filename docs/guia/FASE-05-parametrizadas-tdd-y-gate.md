# FASE 5 — `@ParameterizedTest`, TDD y el pipeline como "gate"

**Qué vas a lograr:** cubrir muchos casos con una sola prueba, practicar TDD (rojo → verde → refactor), y cumplir los requisitos del **Avance 2**.
**Curso:** Semana 5 · P2 "De pruebas repetidas a @ParameterizedTest + gate de CI" y Avance 2.

---

## 1. Conceptos

### Por qué parametrizar
Cuando escribes diez pruebas que solo cambian el número, copias y pegas. Una prueba parametrizada tiene **una** lógica y **una tabla de datos**:
```java
@ParameterizedTest(name = "{0} min -> ${1}")
@CsvSource({
    "0,  0",
    "15, 0",
    "16, 10"
})
void tarifaPorTiempo(int minutos, int esperado) {
    assertEquals(esperado, new Tarifa().calcular(minutos));
}
```
- `@ParameterizedTest`: la prueba se ejecuta **una vez por fila**.
- `@CsvSource`: la fuente de datos; cada cadena es una fila separada por comas. Los valores se convierten solos al tipo del parámetro.
- `name = "{0} min -> ${1}"`: plantilla del nombre de cada ejecución; `{0}` es el primer valor de la fila, `{1}` el segundo, `{index}` el número de ejecución. Sirve para que el IDE muestre `15 min -> $0` en lugar de `[3]`.
  - Ojo: el reporte de consola/Surefire (`.txt`) muestra **solo el índice** `[3]`; el nombre bonito lo ves en el IDE y en el reporte XML. Para ubicar qué fila falló, cuenta la fila (la primera es `[1]`).
- Otras fuentes: `@ValueSource(strings = {...})` (una lista de valores), `@NullAndEmptySource` (agrega `null` y `""`), `@MethodSource` (los datos vienen de un método).
- `Tests run: N` cuenta **cada fila** como una prueba: es tu evidencia de cuántos casos corrieron realmente.

### Análisis de valores frontera
Los errores viven donde cambia la regla. Para cada umbral prueba **n−1, n y n+1**:
| Umbral | Valores a probar |
|---|---|
| gracia de 15 min | 14, 15, 16 |
| fin de la primera hora | 59, 60, 61 |
| siguiente hora adicional | 119, 120, 121 |
| tope de $80 | un valor justo antes y uno muy por encima (600) |

### TDD: rojo → verde → refactor
1. **Rojo:** escribo primero la prueba de la regla; falla porque el código no la cumple todavía.
2. **Verde:** escribo lo mínimo para que pase.
3. **Refactor:** limpio el código con la red de seguridad de las pruebas.
En un PR eso se ve como commits separados: `test:` (rojo) y después `fix:`/`feat:` (verde).

### El pipeline como *gate* (compuerta)
Con CI en cada push y PR, el check ❌ **bloquea la confianza**: nadie debería fusionar con rojo. En la Fase 7 lo harás obligatorio con protección de rama.

---

## 2. Pasos

### Paso 1 — Rama
```powershell
git switch main
git pull
git switch -c feature/parametrizadas
```

### Paso 2 — `TarifaParametrizadaTest` (cubre TODAS las fronteras)
Crea `TarifaParametrizadaTest.java` con **una** prueba parametrizada y una tabla `@CsvSource` con minutos y total esperado según la regla (revisa los ejemplos del comentario de `Tarifa.java`):
`0, 14, 15, 16, 59, 60, 61, 119, 120, 121, 600`.
Calcula tú los valores esperados **a partir de la regla**, no ejecutando el código y copiando lo que devuelve (eso sería "fotografiar" el defecto).
```powershell
mvn -B test -Dtest=TarifaParametrizadaTest
```
Cuenta el `Tests run:` (debe coincidir con tus filas). Si ya corregiste `Tarifa` en la Fase 3, todo debe pasar: ahí está tu red de seguridad.

### Paso 3 — TDD con `Reporte` (aquí hay un defecto que aún no conoces)
Regla: `ocupacionPorcentaje(ocupados, capacidad) = ocupados * 100 / capacidad` (entero, se trunca).
1. **Rojo**: escribe `ReporteTest` con un `@CsvSource` de `(ocupados, capacidad, esperado)`: `0,5,0` · `1,3,33` · `2,3,66` · `3,3,100` · `1,2,50`.
   Más un `assertThrows` para capacidad 0.
   ```powershell
   git add parqueo-api/src/test
   git commit -m "test: ocupacion de Reporte (parametrizada)"
   git push -u origin feature/parametrizadas
   ```
   Se esperan fallos: lee `expected` vs `was` y encuentra la causa tú mismo (pista: ¿qué hace Java con `1 / 3`?).
2. **Verde**: corrige `Reporte.java` en un commit aparte (`fix: ...`).
3. **Refactor**: nota que hay una variable sin uso; elimínala (`refactor: ...`). (SonarQube te la va a marcar en la Fase 7.)
4. Agrega `@ParameterizedTest` + `@CsvSource` también para `etiquetaOcupacion` (49 → BAJA, 50 → MEDIA, 89 → MEDIA, 90 → ALTA).

### Paso 4 — Otras fuentes de datos
Reescribe `PaseDescuentoTest` (Fase 4) para los códigos "válidos pero escritos distinto" con `@ParameterizedTest` + `@ValueSource(strings = {"itson20", "  ITSON20 ", "Itson20"})`, y los "en blanco" con `@NullAndEmptySource` + `@ValueSource(strings = {"   "})`.

### Paso 5 — Checklist del Avance 2 (así se evalúa)
- [ ] **Mínimo 6 pruebas unitarias** de lógica de negocio real (cálculos, validaciones, reglas), con estructura AAA.
- [ ] `@ParameterizedTest` donde hay varios casos.
- [ ] `.github/workflows/ci.yml` que corre `mvn -B test` (o `verify`) en cada push y PR.
- [ ] Un check **ROJO real** y luego **VERDE**, en **commits separados**.
- [ ] PR `avance-2-testing` → `main`, **sin mergear** al entregar, con descripción corta: qué probaron, links al run rojo y verde, quién hizo qué y si usaron IA.
- [ ] En equipo: **todos** los integrantes con commits en el PR.
- Lo que cuenta es el **estado del PR al corte**: commits posteriores no se consideran.

Usa `docs/plantillas/descripcion-pr.md`.

### Paso 6 — Cuando una prueba parametrizada falla
```powershell
mvn -B test -Dtest=TarifaParametrizadaTest
type parqueo-api\target\surefire-reports\mx.itson.devops.parqueo.TarifaParametrizadaTest.txt
```
- `type` muestra un archivo de texto en la terminal.
- Verás `[n]` y los valores. Con eso localizas la fila.

---

## 3. Evidencia a guardar
- [ ] `Tests run:` total (con filas parametrizadas) y salida verde.
- [ ] Link run ROJO / run VERDE de `feature/parametrizadas`.
- [ ] Una explicación escrita (3 líneas) de por qué probaste 14/15/16.

## 4. Para entender de verdad
1. ¿Qué diferencia hay entre `@ValueSource` y `@CsvSource`?
2. ¿Por qué `Tests run:` sube cuando parametrizas aunque haya un solo método?
3. ¿Por qué el valor esperado se calcula de la regla y no copiando lo que devuelve el código?
4. ¿Qué significa que el pipeline sea un *gate*?
