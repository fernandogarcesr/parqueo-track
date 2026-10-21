# FASE 7 — Calidad con JaCoCo y SonarQube Cloud (+ protección de rama)

**Qué vas a lograr:** medir la cobertura de pruebas con JaCoCo, conectar el repo a SonarQube Cloud desde un workflow, entender el Quality Gate, ver cómo falla por defectos reales, corregirlos y volver el gate obligatorio con protección de rama.
**Curso:** Unidad 1 / Avance 1 (workflow de análisis con diagnóstico y fix trazable).

---

## 1. Conceptos

### Análisis estático
Una herramienta **lee tu código sin ejecutarlo** y busca problemas con reglas. SonarQube clasifica los hallazgos:
| Tipo | Qué es | Ejemplo en este proyecto |
|---|---|---|
| **Bug** | probablemente se comporta mal | comparar cadenas con `==` |
| **Vulnerability** | falla de seguridad explotable | credenciales escritas en el código |
| **Security Hotspot** | código sensible que **un humano debe revisar** | contraseña escrita en una constante |
| **Code Smell** | deuda de mantenimiento | `if (x == true)`, variable sin uso |
Cada hallazgo trae una **regla** (`java:S1481`...) con explicación y ejemplo de cómo corregirlo. (Los códigos exactos pueden variar con la versión del analizador.)

### Cobertura (coverage)
Porcentaje de líneas/ramas de tu código que **ejecutaron tus pruebas**.
- **JaCoCo** la mide: el goal `prepare-agent` engancha un *agente* a la JVM de las pruebas (por eso va ANTES de `test`); el goal `report` (ligado a la fase `test`) escribe `target/site/jacoco/jacoco.xml` y un HTML navegable.
- **SonarQube Cloud no mide la cobertura**: solo **lee** el XML de JaCoCo. Si el XML no existe o la ruta es incorrecta, Sonar muestra 0 %.
- Cobertura alta ≠ buenas pruebas (puedes ejecutar una línea sin verificar nada), pero **cobertura baja = código sin red de seguridad**.

### Código nuevo (*New Code*) y Quality Gate
- **New Code**: lo que cambió desde tu referencia (por ejemplo, la versión anterior o los últimos N días). Sonar exige calidad sobre lo **nuevo**, para no castigarte por deuda antigua.
- **Quality Gate**: un conjunto de condiciones que deciden **aprobado / reprobado**. El perfil por omisión *Sonar way* suele pedir, sobre código nuevo: sin bugs ni vulnerabilidades nuevos, todos los hotspots nuevos revisados, **cobertura ≥ 80 %**, duplicación ≤ 3 %. (Léelo tú mismo en tu proyecto: Quality Gate → condiciones.)
- `-Dsonar.qualitygate.wait=true` hace que el comando de Maven **espere** el resultado y **falle el paso** si el gate no pasa; así el check de GitHub sale ❌.

### Piezas y cómo se conectan
```
 tu código ──► GitHub Actions (runner)
                 ├─ mvn verify  ──► JaCoCo ──► jacoco.xml
                 └─ sonar:sonar ──(SONAR_TOKEN)──► SonarQube Cloud
                                                    ├─ analiza código + lee jacoco.xml
                                                    ├─ aplica el Quality Gate
                                                    └─ responde pasó/falló ──► check del PR
```
- **Organización** y **Project Key**: identifican *a quién* pertenece el proyecto y *cuál* es en Sonar.
- **`SONAR_TOKEN`**: tu "contraseña" para que el runner pueda enviar el análisis. Se guarda como **secret** del repo, nunca en el código.
- `fetch-depth: 0` en el checkout: Sonar necesita el **historial completo** para saber qué es "código nuevo"; el checkout por omisión trae solo 1 commit.
- **Métodos de análisis**: *Automatic Analysis* (Sonar analiza solo, sin cobertura) vs **CI-based** (tu workflow lo envía). Si usas el workflow, **desactiva Automatic Analysis** o verás un error de conflicto.

---

## 2. Pasos

### Paso 1 — Mira la cobertura local con JaCoCo
```powershell
mvn -B verify
start parqueo-api\target\site\jacoco\index.html
```
- `start archivo` abre el archivo con el programa predeterminado (el navegador).
- Navega por paquete → clase. Verde = ejecutada, rojo = nunca ejecutada, amarillo = ramas a medias. Identifica qué clases tienen 0 % (por ejemplo `ConfigAdmin`).

### Paso 2 — Cuenta y organización en SonarQube Cloud
1. Entra a **sonarcloud.io** → *Log in with GitHub* → autoriza.
2. *Import an organization* desde tu cuenta de GitHub (o crea una); elige el plan **Free**.
3. *Analyze new project* → selecciona `parqueo-track`.
4. Elige cómo se define el código nuevo (la opción por omisión está bien para empezar).
5. En el proyecto → **Administration → Analysis Method**: desactiva *Automatic Analysis* y elige **With GitHub Actions**. El asistente te muestra el *project key* y la *organization key*: apúntalos.

### Paso 3 — Token y secret
1. En SonarQube Cloud: tu avatar → **My Account → Security → Generate Tokens**. Copia el token (solo se muestra una vez).
2. En GitHub: repo → **Settings → Secrets and variables → Actions → New repository secret**: nombre `SONAR_TOKEN`, valor = el token.
> Nunca pegues el token en un archivo del repo ni en un chat.

### Paso 4 — El workflow de calidad
```powershell
git switch main
git pull
git switch -c feature/sonar
Copy-Item docs\plantillas\calidad.yml .github\workflows\calidad.yml
```
- Edita `.github/workflows/calidad.yml` y cambia `TU_ORGANIZACION` y `TU_PROJECT_KEY`.
- Edita también `pom.xml` (padre): `sonar.organization` y `sonar.projectKey` en `<properties>` (deben coincidir).
- Lee el workflow línea por línea. Preguntas: ¿por qué `fetch-depth: 0`? ¿qué hace `actions/cache` con `~/.sonar/cache`? ¿cuál es la diferencia entre `GITHUB_TOKEN` y `SONAR_TOKEN`? ¿por qué el comando de Maven empieza con `mvn -B verify` y después `org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`?
```powershell
git add .
git commit -m "ci: agregar workflow de analisis con SonarQube Cloud"
git push -u origin feature/sonar
```
Abre un PR hacia `main`.

### Paso 5 — Lee el resultado (se espera Quality Gate en ROJO)
En el PR verás un check de calidad ❌ y, en SonarQube Cloud (proyecto → rama/PR), el resumen. Anota para cada hallazgo: tipo, regla, archivo/línea y por qué lo marcó. Probablemente verás:
- Cobertura insuficiente en código nuevo (si aún no cubres `Reporte`, `ConfigAdmin`, `ParqueoServer`...).
- Credenciales en el código (`ADMIN_PASSWORD`).
- Cadenas comparadas con `==` (si aún no corregiste `PaseDescuento`).
- `if (x == true)` redundante y variable local sin usar.

### Paso 6 — Corrige con commits trazables (uno por hallazgo)
| Hallazgo | Cómo atacarlo | Commit sugerido |
|---|---|---|
| Credenciales en el código | no escribirlas: leerlas de configuración externa, p. ej. `System.getenv("ADMIN_PASSWORD")` y que el código falle/rechace si no está definida. (Si la contraseña real estuvo en el historial, se **rota**) | `fix: leer ADMIN_PASSWORD de variable de entorno` |
| `==` con cadenas | ya lo arreglaste en la Fase 4 | — |
| Booleano redundante | `return adentro.contains(p);` | `refactor: simplificar estaDentro` |
| Variable sin uso | eliminarla | `refactor: quitar variable sin uso` |
| Cobertura baja | más pruebas AAA/parametrizadas para lo que no está cubierto (`ConfigAdmin`, `Reporte`, `ParqueoServer.parsearQuery`...) | `test: ...` |
Después de cada `push`, el PR se re-analiza. Repite hasta que el Quality Gate pase.
**Regla:** no bajes las condiciones del gate ni excluyas archivos de cobertura solo para "ponerlo verde" (eso es lo mismo que cambiar un `assert`). Excluir con criterio (por ejemplo `Main.java`, que solo arranca el servidor) es legítimo si lo **justificas por escrito** en el PR.

### Paso 7 — Protección de rama (que el verde sea obligatorio)
Con al menos un run de **cada** workflow ya ejecutado:
1. GitHub → **Settings → Branches** (o *Rules → Rulesets*) → regla para `main`.
2. Activa: **Require a pull request before merging** y **Require status checks to pass before merging**.
3. En la lista de checks elige **exactamente** los nombres de los jobs: `Build y pruebas (Maven)` y `Build, pruebas y análisis Sonar` (si no aparecen, es porque aún no corrieron).
4. Activa **Require branches to be up to date** (opcional) y **no permitas saltarse las reglas**.
5. Prueba: abre un PR con una prueba roja a propósito → el botón de merge debe estar **bloqueado**. Corrige → se habilita.
> La protección de ramas en repos privados puede requerir plan de pago; en repos públicos es gratuita. Por eso el repo del curso es público y sin secretos.

### Paso 8 — Mergea y revisa la rama principal
Cuando todo esté en ✅, mergea. En SonarQube Cloud abre el panel de `main`: cobertura global, hotspots, deuda técnica.
Opcional: copia el *badge* de calidad al `README.md`.

---

## 3. Evidencia a guardar
- [ ] Captura del Quality Gate ❌ con los hallazgos y link al run rojo.
- [ ] Un commit por hallazgo + captura del Quality Gate ✅ y link al run verde.
- [ ] Captura de la regla de protección de rama y del botón de merge bloqueado.
- [ ] Tu explicación de "cobertura de JaCoCo vs lo que lee Sonar".

## 4. Errores comunes
| Síntoma | Causa | Arreglo |
|---|---|---|
| `You are running CI analysis while Automatic Analysis is enabled` | los dos métodos activos | desactiva *Automatic Analysis* en Administration → Analysis Method |
| `Not authorized` / `Invalid token` | `SONAR_TOKEN` mal copiado o sin secret | vuelve a generar y guardar el secret |
| Cobertura 0 % | no se generó `jacoco.xml` o `verify` no corrió antes del escáner | revisa `parqueo-api/target/site/jacoco/jacoco.xml` y el orden del comando |
| `Project not found` | `projectKey` u `organization` incorrectos | cópialos exactamente de SonarQube Cloud |
| El análisis tarda o se queda en blanco | sin `fetch-depth: 0` | agrégalo al checkout |
| El PR de un fork no tiene el secret | GitHub no expone secrets a forks | analiza ramas del mismo repo |

## 5. Para entender de verdad
1. ¿Por qué JaCoCo y Sonar son dos herramientas distintas y cómo se pasan la información?
2. ¿Qué es el código nuevo y por qué el gate se mide sobre él?
3. ¿Por qué `fetch-depth: 0`?
4. ¿Qué significa que un check sea "obligatorio" y dónde se configura?
5. ¿Qué diferencia hay entre un Bug, una Vulnerability y un Hotspot?
