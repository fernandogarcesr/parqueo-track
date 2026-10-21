# FASE 0 — Preparación: herramientas, repositorio y primer push

**Qué vas a lograr:** dejar tu entorno listo, subir el proyecto a GitHub y ver el primer CI (que va a salir ROJO a propósito).
**Curso:** Semana 1 (repo bootstrap, baseline, Value Stream AS-IS, working agreement).

---

## 1. Conceptos

### El delivery system (la cadena completa)
DevOps no es "usar herramientas": es mejorar la capacidad de **entregar y operar software** de forma frecuente y confiable.
Esa entrega es una cadena:

```
código → build → pruebas → artefacto → entrega → deployment → operación → feedback
```

En este proyecto vas a recorrer **todos** esos eslabones: compilar (Maven), probar (JUnit), vigilar calidad (SonarQube Cloud),
guardar el artefacto (Nexus), empaquetarlo (Docker) y correrlo en la nube (AWS).

### Repositorio = fuente de la verdad (source of truth)
Todo lo que define el sistema (código, configuración, documentación) vive **versionado** en el repo. Regla de oro de la unidad:
> repo → commit → **fresh clone** (una copia limpia debe compilar y pasar pruebas sin magia).

Si algo solo funciona "en mi compu", no es reproducible.

### Git: tres lugares donde vive un cambio
1. **Working directory** (carpeta de trabajo): los archivos que editas.
2. **Staging area** (índice): lo que vas a incluir en el próximo commit (`git add`).
3. **Repositorio local**: los commits guardados (`git commit`).
Y además el **remoto** (`origin` = GitHub), al que envías con `git push`.

### Archivos *tracked* vs *untracked*
- *Untracked*: Git los ve pero no los sigue todavía.
- *Tracked*: ya están en un commit; Git detecta sus cambios.
- `.gitignore` lista lo que Git debe ignorar (como `target/`, que Maven regenera).

---

## 2. Pasos

### Paso 1 — Verifica las herramientas
```powershell
java -version
mvn -v
git --version
docker --version
```
- Cada comando imprime la versión instalada; si da "no se reconoce", falta instalar o falta el PATH.
- `java -version` → debe decir **21**. `mvn -v` → Maven **3.9+** y, en la línea "Java version", también 21 (si Maven usa otro Java, revisa `JAVA_HOME`).
- Docker lo necesitas desde la Fase 8 (Docker Desktop abierto, con "Engine running").

### Paso 2 — Descomprime el proyecto
Ponlo en una ruta corta y **fuera de OneDrive**: `C:\Proyectos\parqueo-track`. (OneDrive sincroniza archivos mientras Maven escribe y causa errores raros.)
```powershell
cd C:\Proyectos\parqueo-track
dir
```
- `cd` cambia de carpeta. `dir` lista el contenido: debes ver `pom.xml`, `parqueo-api`, `avisos`, `docs`, `README.md`.

### Paso 3 — Tu identidad en Git (una sola vez en la compu)
```powershell
git config --global user.name "Tu Nombre"
git config --global user.email "el-correo-de-tu-cuenta-de-GitHub"
git config --global init.defaultBranch main
git config --global core.autocrlf true
```
- `git config` guarda ajustes. `--global` = para todos tus repos (sin él, solo para el repo actual).
- Nombre y correo quedan escritos en **cada commit**. Si el correo no está vinculado a tu GitHub, tus commits no cuentan como tuyos (importante en los avances: "todos los integrantes deben tener commits").
- `core.autocrlf true` evita el aviso "LF will be replaced by CRLF" (Windows usa fin de línea CRLF; Linux LF). Es inofensivo.
- Para revisar: `git config --global --list`.

### Paso 4 — Crea el repositorio vacío en GitHub
GitHub → **New repository**:
- Nombre: `parqueo-track`
- Visibilidad: **Public** (necesario para usar protección de ramas y SonarQube Cloud gratis; por eso el repo NO debe contener secretos).
- **No** marques README, .gitignore ni licencia (ya vienen en tu carpeta).

### Paso 5 — Primer commit y push
```powershell
git init -b main
git status
git add .
git status
git commit -m "chore: commit inicial"
git remote add origin https://github.com/TU_USUARIO/parqueo-track.git
git remote -v
git push -u origin main
```
Qué hace cada uno:
- `git init -b main` crea el repo local (la carpeta oculta `.git`) y nombra la rama inicial `main` (`-b` = *branch*). Antes de este comando, `git status` dice "fatal: not a git repository".
- `git status` muestra qué está *untracked*, *staged* o *modified*. Úsalo **todo el tiempo**.
- `git add .` pasa TODO lo no ignorado a la staging area (el `.` = "la carpeta actual"). Verifica con `git status` que ya no esté en rojo.
- `git commit -m "..."` guarda una foto del staging con un mensaje. Convención del curso: `tipo: descripción` (`chore`, `feat`, `fix`, `test`, `ci`, `build`, `docs`, `refactor`).
- `git remote add origin URL` registra el remoto llamado `origin` (es solo un alias de la URL).
- `git remote -v` muestra las URLs de fetch/push configuradas.
- `git push -u origin main` envía `main` a `origin`. `-u` (*set upstream*) recuerda la relación para que después baste `git push` / `git pull`.

Si te pide autenticación, se abre una ventana del navegador para iniciar sesión en GitHub.

### Paso 6 — Mira tu primer CI (¡va a salir rojo!)
En GitHub → pestaña **Actions** → run "CI" del commit inicial. Verás una ❌.
- Abre el job **Build y pruebas (Maven)** → el paso "Compilar, probar y empaquetar" → baja al final del log.
- Busca un mensaje parecido a `package org.junit.jupiter.api does not exist` en `ParqueoSmokeTest.java`.
- **No lo arregles todavía**: ese rojo es la evidencia de la Fase 1.
- Guarda el link del run (lo vas a citar).

### Paso 7 — Documentos de la Semana 1
Copia las plantillas a `docs/` y llénalas con lo que viviste (en equipo se hace en Sprint 0):
```powershell
Copy-Item docs\plantillas\working-agreement.md docs\working-agreement.md
Copy-Item docs\plantillas\value-stream-as-is.md docs\value-stream-as-is.md
```
- `Copy-Item origen destino` copia un archivo.
- **Value Stream AS-IS** = cómo funciona REALMENTE hoy tu flujo (repo → clone → IDE → build → pruebas → ejecución): dónde hay trabajo manual, esperas, *handoffs*, retrabajo y feedback. Anota tiempos reales (cuánto tardaste instalando, cuánto esperó el CI...).
- **Working agreement** = pocas reglas verificables de cómo trabajas.

---

## 3. Evidencia a guardar
- [ ] Salida de `git log --oneline` con tu primer commit.
- [ ] Link al run ROJO de CI (compilación).
- [ ] `docs/working-agreement.md` y `docs/value-stream-as-is.md` (borrador).
- [ ] Primeras filas de tu `bitacora-comandos.md`.

## 4. Errores comunes
| Síntoma | Causa | Qué hacer |
|---|---|---|
| `fatal: not a git repository` | no has hecho `git init` o estás en otra carpeta | `cd` al proyecto y `git init -b main` |
| `src refspec main does not match any` | intentaste push sin ningún commit | haz `git add .` y `git commit` primero |
| `remote origin already exists` | ya agregaste el remoto | `git remote set-url origin URL` |
| `Authentication failed` | credenciales viejas | cierra sesión del Administrador de credenciales de Windows y reintenta |
| `LF will be replaced by CRLF` | solo es un aviso de fin de línea | ignóralo (o `core.autocrlf true`) |
| El push dice rama `master` | tu Git creó `master` | `git branch -M main` y repite el push |

## 5. Para entender de verdad (respóndete con tus palabras)
1. ¿Qué diferencia hay entre `git add` y `git commit`?
2. ¿Qué es `origin`? ¿Qué cambia si no usas `-u` en el primer push?
3. ¿Por qué el repo debe poder clonarse limpio y compilar?
