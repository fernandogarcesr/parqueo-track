# FASE 3 — Ramas, Pull Requests y tu primer rojo→verde real

**Qué vas a lograr:** entender estrategias de branching, provocar y resolver un conflicto en un sandbox, escribir un ADR, y llevar un cambio a `main` con un PR que muestre un check ROJO real y luego VERDE.
**Curso:** Semana 2 (configuración y branching), Semana 3 (PR y code review), Semana 4 P2 (rojo→verde en CI).

---

## 1. Conceptos

### Gestión de configuración
Controlar y hacer **trazables** los cambios de los artefactos que definen un sistema (código, configuración, documentación). La estrategia de branching decide **cómo y cuándo** esos cambios vuelven a integrarse.
Modelo mental: **estado versionado → cambio trazable (quién, qué, por qué) → integración frecuente**.

### Divergencia y *merge debt*
Mientras más tiempo vive una rama separada de `main`, más se aleja y más duele integrarla ("dos ramas vivieron 12 días separadas: el merge rompe 14 archivos"). El problema no es Git: es el **tamaño del lote de integración**.

### Estrategias de branching (y sus trade-offs)
| Estrategia | Idea | Ventaja | Costo |
|---|---|---|---|
| **Trunk-Based** | ramas muy cortas o commits directos a `main` con controles | feedback temprano, lotes pequeños | exige disciplina y automatización |
| **GitHub Flow** | `main` siempre desplegable + feature branch + Pull Request + merge | simple, compatible con PR | se degrada si las ramas viven demasiado |
| **GitFlow** | `develop`, `release`, `hotfix`... | separación explícita de releases | más ramas, más merges, más divergencia |
| **GitLab Flow** | ramas por entorno/release | se adapta a entornos | más política y coordinación |

Tres mitos a evitar: "GitFlow siempre es más profesional", "más ramas = más control", "Trunk-Based significa sin code review".
Este curso usa **GitHub Flow**.

### Pull Request (PR)
Una propuesta de cambio: "quiero fusionar la rama X en `main`". Sirve para: ver el diff, ejecutar el CI sobre ese cambio, **revisar** (code review) y discutir antes de integrar.

### Cómo se fusiona (merge)
| Tipo | Qué pasa | Historia |
|---|---|---|
| **Fast-forward** | `main` no se movió: el puntero solo avanza | línea recta, sin commit de merge |
| **Merge commit** (`--no-ff`) | se crea un commit que une ambas historias | conserva la rama visible |
| **Squash** | todos los commits de la rama se aplastan en uno | historia limpia pero pierdes los commits intermedios |
| **Rebase** | reescribe tus commits encima de `main` | línea recta, pero **reescribe historia** (no lo hagas en ramas compartidas) |

Para esta práctica usa **merge commit**: así el log conserva el rojo y el verde como commits separados.

### Rojo real → verde
Un rojo es "real" cuando lo causa un **defecto de comportamiento** (una prueba que dice la verdad y falla), no un error de sintaxis ni un `assert` falso. Regla de oro: **nunca cambies el `assert` para obtener un verde** (eso es manipular la prueba). Se corrige el código o se discute la regla.

---

## 2. Laboratorio A — Sandbox de conflictos (repo aparte, no el del proyecto)

Objetivo: ver con tus ojos cómo nace un conflicto.
```powershell
cd C:\Proyectos
mkdir sandbox-branching
cd sandbox-branching
git init -b main
Set-Content -Path tarifa.txt -Value "tarifa base: 10"
git add .
git commit -m "chore: base"
```
- `mkdir` crea carpeta. `Set-Content -Path archivo -Value texto` crea/sobrescribe un archivo con ese texto.

Dos personas cambian **la misma línea** desde la misma base:
```powershell
git switch -c rama-a
Set-Content -Path tarifa.txt -Value "tarifa base: 12"
git commit -am "feat: A sube la tarifa"
git switch main
git switch -c rama-b
Set-Content -Path tarifa.txt -Value "tarifa base: 15"
git commit -am "feat: B sube la tarifa"
git switch main
```
Integra:
```powershell
git merge rama-a
git merge rama-b
git status
Get-Content tarifa.txt
```
- `git merge rama-a` → *Fast-forward* (main no se había movido).
- `git merge rama-b` → **CONFLICT**: ambas ramas cambiaron la misma línea desde la base y Git no puede elegir.
- `git status` dice `both modified` y `Get-Content` muestra los **marcadores**:
```
<<<<<<< HEAD
tarifa base: 12        ← lo que ya estaba en main (rama-a)
=======
tarifa base: 15        ← lo que trae rama-b
>>>>>>> rama-b
```
Resuélvelo: abre el archivo, deja SOLO la línea final correcta (borra los 3 marcadores), y:
```powershell
git add tarifa.txt
git commit -m "merge: resolver conflicto de tarifa"
git log --graph --oneline --all
```
- `git add` le dice a Git "ya lo resolví". `git commit` crea el commit de merge.
- `git log --graph --oneline --all` dibuja el árbol de todas las ramas (`--graph` ASCII, `--oneline` una línea por commit, `--all` todas las ramas).
- Si te asustas a la mitad: `git merge --abort` regresa al estado previo al merge.

Repite con `git rebase`: crea otra rama `rama-c` desde un commit anterior, y haz `git rebase main`. Observa que el historial queda en línea recta pero los hashes de tus commits **cambian** (se reescribieron).

## 3. Laboratorio B — ADR de branching
```powershell
cd C:\Proyectos\parqueo-track
Copy-Item docs\plantillas\adr-0001-branching.md docs\adr-0001-branching.md
```
Llénalo comparando al menos 3 estrategias. Tu pregunta guía: *¿qué estrategia escogerías para un producto que despliega diario frente a uno regulado con releases cada 3 meses?* Justifica con contexto, evidencia (tu sandbox) y trade-off.

---

## 4. Laboratorio C — Tu primer PR con rojo real y verde

### Paso 1 — Rama y pruebas
```powershell
git switch main
git pull
git switch -c feature/tarifa-frontera
```
Crea `parqueo-api/src/test/java/mx/itson/devops/parqueo/TarifaFronteraTest.java`:
```java
package mx.itson.devops.parqueo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TarifaFronteraTest {

    @Test
    void quinceMinutosSiguenSiendoGratis() {
        // Arrange
        Tarifa tarifa = new Tarifa();
        // Act
        int total = tarifa.calcular(15);
        // Assert: la regla dice que el minuto 15 se INCLUYE en la gracia
        assertEquals(0, total);
    }

    @Test
    void unaFraccionDeHoraSeCobraComoHoraCompleta() {
        Tarifa tarifa = new Tarifa();

        int total = tarifa.calcular(61);

        // 10 de la primera hora + 8 por la fracción adicional
        assertEquals(18, total);
    }
}
```
**Frontera** = el valor justo donde cambia la regla. Se prueban `n-1`, `n` y `n+1` porque ahí viven los errores de "uno de más / uno de menos" (*off-by-one*).

### Paso 2 — Commit del ROJO y push
```powershell
mvn -B test
git add .
git commit -m "test: fronteras de 15 minutos y fraccion de hora"
git push -u origin feature/tarifa-frontera
```
`mvn -B test` en tu terminal ya debe salir **BUILD FAILURE** con 2 pruebas fallidas. Léelas:
`TarifaFronteraTest.quinceMinutosSiguenSiendoGratis: expected: <0> but was: <10>`.
Cada fallo te da: **nombre de la prueba, valor esperado y valor obtenido**. Un buen diagnóstico siempre cita los tres.

### Paso 3 — Abre el PR
GitHub mostrará el banner **Compare & pull request** (o: Pull requests → New → base `main` ← compare `feature/tarifa-frontera`).
- Pega `docs/plantillas/descripcion-pr.md` en la descripción y llena lo que ya sabes.
- Espera el CI: el check sale ❌. **Guarda el link de ese run** (es tu evidencia del rojo).

### Paso 4 — Corrige el código (commit del VERDE)
Edita `Tarifa.java` (sin tocar las pruebas):
1. El límite de gracia es `<= 15`, no `< 15`.
2. Las horas adicionales se cuentan **hacia arriba** (hora o fracción): usa `Math.ceilDiv(minutos - 60, 60)` en vez de la división entera.
(`Math.ceilDiv` divide redondeando hacia arriba; existe desde Java 18.)
```powershell
mvn -B test
git add parqueo-api
git commit -m "fix: tiempo de gracia incluye 15 min y fraccion de hora se cobra completa"
git push
```
El PR se actualiza solo y el check pasa a ✅. **Guarda el link del run verde.**

### Paso 5 — Code review y merge
- Pestaña **Files changed**: revisa tu propio diff como si fuera de otro. ¿Hay algo que no puedas explicar?
- **Merge pull request** (merge commit; en el menú desplegable, evita *squash* esta vez).
```powershell
git switch main
git pull
git branch -d feature/tarifa-frontera
git log --graph --oneline -8
```
- `git branch -d` borra la rama local ya fusionada (`-d` se niega si no está mergeada).
- En el log verás el commit rojo, el verde y el commit de merge.

---

## 5. Evidencia a guardar
- [ ] Sandbox: salida de `git log --graph --oneline --all` con el conflicto resuelto.
- [ ] `docs/adr-0001-branching.md`.
- [ ] PR con: link del run ROJO, link del run VERDE, y los dos commits separados.

## 6. Para entender de verdad
1. ¿Por qué un conflicto surge si ambos cambiaron la MISMA línea y no si cambiaron líneas distintas?
2. ¿Por qué la prueba de frontera no se escribe con el valor 30?
3. Si cambiaras `assertEquals(0, ...)` por `assertEquals(10, ...)` para tener verde, ¿qué estarías demostrando?
4. ¿Qué ganas y qué pierdes al hacer *squash* vs *merge commit*?
