# FASE 2 — Integración Continua con GitHub Actions

**Qué vas a lograr:** entender cada línea de `ci.yml`, leer un run como detective y hacer 3 experimentos que rompen el pipeline a propósito.
**Curso:** Unidad 1 (primer workflow) + Semana 4 (el mismo build en CI).

---

## 1. Conceptos

### CI es una práctica de equipo, no un archivo `.yml`
> Integrar cambios pequeños a la línea principal varias veces al día, verificar cada integración automáticamente y **reparar el rojo antes de acumular más cambios**.

La herramienta (GitHub Actions) solo ejecuta la verificación; **qué** se verifica lo decide el equipo. Un pipeline verde sin pruebas reales no demuestra nada ("tu pipeline puede estar verde y el negocio roto"; lo vas a vivir con los errores sembrados).

### El vocabulario de GitHub Actions
| Término | Qué es | Analogía |
|---|---|---|
| **Workflow** | archivo `.yml` en `.github/workflows/` con las instrucciones | la receta |
| **Trigger (`on`)** | el evento que lo dispara: `push`, `pull_request`, `workflow_dispatch`, `schedule`... | "cuando suene el timbre" |
| **Job** | grupo de pasos que corre en UNA máquina | un cocinero |
| **Runner** | la máquina virtual (`ubuntu-latest`) que GitHub presta; **arranca vacía** y se destruye al terminar | una cocina limpia por turno |
| **Step** | un paso dentro del job | una instrucción de la receta |
| **`uses`** | usar una *acción* ya hecha (`actions/checkout@v4`) | importar una librería |
| **`run`** | ejecutar un comando de terminal | escribirlo yo |
| **Artifact** | archivos que el run guarda para descargarlos después | lo que dejas en la mesa |
| **Secret** | valor guardado cifrado (`${{ secrets.NOMBRE }}`); nunca sale en logs | caja fuerte |
| **Check** | el resultado (✅/❌) que aparece en el commit o en el PR | semáforo |

### YAML en 30 segundos
- Sangría con **espacios** (jamás tabuladores). Un espacio de más o de menos cambia el significado.
- `clave: valor`, listas con `- `, comentarios con `#`.
- `name:` de job y de step es solo lo que se *ve*; el **id** del job (`build:`) es el identificador interno.

### Por qué el runner "arranca vacío"
El runner no tiene tu código, ni Java, ni Maven configurado. Por eso `ci.yml` empieza con **checkout** (descargar el repo) y **setup-java** (instalar Java 21).
Esto es el opuesto de "funciona en mi máquina": el CI **reconstruye todo desde cero** cada vez. Si pasa ahí, es reproducible.

### `mvn -B verify` vs `mvn -B test`
`verify` llega más lejos (compila, prueba, **empaqueta** y verifica). En CI usamos `verify` para detectar también problemas de empaquetado.
(El Avance 2 del curso pide `mvn -B test` como mínimo; `verify` lo incluye.)

### `if: always()`
Por omisión, un paso **se salta** si el anterior falló. Con `if: always()` corre siempre. El paso que sube los reportes lo necesita: los reportes importan más cuando algo falló.

---

## 2. Pasos

### Paso 1 — Lee `ci.yml` línea por línea
Abre `.github/workflows/ci.yml`. Está comentado en primera persona. Contesta por escrito (en tu bitácora):
1. ¿Qué eventos lo disparan? ¿Qué pasa si haces push a una rama `feature/x`? ¿Y si abres un PR hacia `main`?
2. ¿Qué pasaría si quitas el paso `checkout`?
3. ¿Qué hace `cache: maven` y por qué el segundo run suele ser más rápido?
4. ¿Por qué el último paso lleva `if: always()`?

### Paso 2 — Explora un run
GitHub → **Actions** → click en un run → click en el job:
- Abre cada paso (flechita). Fíjate en el tiempo de cada uno.
- En el paso de Maven, busca: `Tests run:`, `BUILD SUCCESS` / `BUILD FAILURE`, `Reactor Summary`.
- En la parte baja de la página del run: **Artifacts → surefire-reports** (descárgalo y ábrelo).
- Cabecera del run: quién lo disparó, qué commit (`SHA`), cuánto duró.

### Paso 3 — Dispáralo a mano
Actions → CI → **Run workflow** (posible gracias a `workflow_dispatch`). Úsalo cuando quieras re-ejecutar sin hacer un commit.

### Paso 4 — Tres experimentos (en una rama desechable)
```powershell
git switch main
git pull
git switch -c exp/ci-experimentos
```
- `git pull` trae lo último de `main`. Ojo: `pull` actúa sobre la rama **en la que estás**.

**Experimento A — el runner vacío.** Comenta el paso `Descargar el código` (pon `#` al inicio de cada una de sus líneas), commit y push:
```powershell
git commit -am "exp: quitar checkout"
git push -u origin exp/ci-experimentos
```
Verás un error del estilo `there is no POM in this directory`. **Lección:** el runner nace vacío.
(`git commit -am` = `add` de los archivos ya rastreados + `commit`.)

**Experimento B — YAML roto.** Restaura el paso y quita 2 espacios de sangría a la línea `java-version`. Commit y push. GitHub marca el workflow como inválido (`Invalid workflow file`) y ni siquiera arranca. **Lección:** la sangría es sintaxis.

**Experimento C — `if: always()`.** Restaura todo. En el paso de Maven cambia el comando a `mvn -B verify -Dno.existe` (opción inválida), y mira si el paso de reportes se ejecuta. Luego quita el `if: always()`, repite, y compara. **Lección:** qué hace cada condición.

### Paso 5 — Limpia los experimentos
```powershell
git switch main
git branch -D exp/ci-experimentos
git push origin --delete exp/ci-experimentos
```
- `git branch -D` borra la rama local (`-D` fuerza aunque no esté mergeada).
- `git push origin --delete rama` la borra en GitHub.

### Paso 6 — Introduce el concepto de "verde que miente"
Con `main` en verde, abre la app y comprueba un caso con la regla de negocio:
```powershell
mvn -B package
java -jar parqueo-api\target\parqueo-api-1.0.0-SNAPSHOT.jar
```
En otra terminal: `curl.exe "http://localhost:8080/api/cobro?minutos=15"`
La regla dice que 15 minutos son **$0** (gracia), pero responde **10** y el CI está verde. ¿Por qué nadie se enteró? Porque **no hay pruebas** de esa regla. Eso es lo que atacan las Fases 3 a 5.

---

## 3. Evidencia a guardar
- [ ] Tus respuestas del Paso 1.
- [ ] Capturas/links de los 3 experimentos (cada uno rojo por una razón distinta).
- [ ] Descarga del artifact `surefire-reports`.

## 4. Para entender de verdad
1. ¿Cuál es la diferencia entre un workflow, un job y un step?
2. ¿Por qué `uses: actions/checkout@v4` y `run: mvn ...` son cosas distintas?
3. ¿Qué evidencia tiene un equipo de que su software funciona si el pipeline es verde pero no hay pruebas?
4. ¿Qué significa "reparar el rojo antes de acumular más cambios"?
