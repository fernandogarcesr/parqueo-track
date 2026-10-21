# FASE 8 — Gestión de artefactos: Nexus y `mvn deploy`

**Qué vas a lograr:** levantar tu propio Nexus con Docker, publicar el `.jar` de ParqueoTrack con `mvn deploy`, entender SNAPSHOT vs RELEASE y diagnosticar los errores típicos de publicación.
**Curso:** Semana 6 (lunes) · Avance 3, primera mitad.

---

## 1. Conceptos

### Qué es un artefacto y por qué guardarlo
El **artefacto** es lo que produce el build (aquí `parqueo-api-1.0.0-SNAPSHOT.jar`). Mandarlo por chat ("el JAR que nadie puede volver a construir") no es evidencia ni es trazable. Un **repositorio de artefactos** guarda cada versión, quién la subió y su huella digital, y otros proyectos (o el pipeline) lo consumen **por referencia** en vez de copiarlo a mano.

### Nexus Repository
Un servidor que guarda artefactos de distintos formatos (Maven, Docker, npm...). Tipos de repositorio:
| Tipo | Qué es | Ejemplo por omisión |
|---|---|---|
| **hosted** | repo propio donde TÚ publicas | `maven-releases`, `maven-snapshots` |
| **proxy** | caché de un repo remoto | `maven-central` |
| **group** | una "vista" que junta varios repos en una sola URL | `maven-public` |
Para publicar siempre apuntas a un **hosted**. Apuntar a un *group* o a un *proxy* da error (405).

### SNAPSHOT vs RELEASE
| | SNAPSHOT (`1.0.0-SNAPSHOT`) | RELEASE (`1.0.0`) |
|---|---|---|
| Significado | versión **en desarrollo** | versión **final** |
| ¿Se puede volver a publicar? | **Sí**; Nexus guarda cada subida con marca de tiempo (`1.0.0-20261019.171204-1.jar`) | **No**: es inmutable (así `1.0.0` siempre es el mismo archivo) |
| Repo destino | `maven-snapshots` | `maven-releases` |
Maven decide solo según el sufijo de la versión. Elegir una "a propósito" significa justificarla: *"uso SNAPSHOT porque el proyecto sigue cambiando"* o *"uso RELEASE porque esta versión se va a demostrar y no debe cambiar"*.

### Las dos mitades de la conexión
- **`pom.xml` → `<distributionManagement>`**: A DÓNDE publicar (URLs). Se sube a GitHub.
- **`settings.xml` (en tu carpeta `.m2`)**: CON QUÉ credenciales. **Nunca** va al repo.
- Se unen por el **`<id>`**: el `<id>` del repo en el pom debe ser idéntico al `<id>` del `<server>` en settings. Si no coinciden, Maven no manda credenciales (error 401).

### `install` vs `deploy`
- `mvn install` copia el artefacto a tu repositorio **local** (`C:\Users\TU_USUARIO\.m2\repository\...`).
- `mvn deploy` lo sube al repositorio **remoto** (Nexus). Recorre antes compile → test → package → verify → install.
- **`deploy` no despliega la app en un servidor**: solo publica el artefacto.

### Docker: contenedor vs volumen (importante para no perder datos)
- Lo que escribe un contenedor vive en su capa efímera: si lo borras (`docker rm`), se pierde.
- Un **volumen** (`-v nombre:/ruta`) es almacenamiento aparte que **sobrevive** al contenedor. Nexus guarda todo en `/nexus-data`, por eso se monta en un volumen.
- `docker stop` apaga (los datos se conservan); `docker start` lo enciende de nuevo; `docker rm` destruye el contenedor.

---

## 2. Pasos

### Paso 1 — Levanta tu Nexus de práctica
(Usa nombres y puertos distintos a los de tu Nexus del proyecto en equipo, para no mezclarlos.)
```powershell
docker volume create nexus-lab-data
docker run -d --name nexus-lab -p 8091:8081 -p 8092:8092 -v nexus-lab-data:/nexus-data sonatype/nexus3
docker logs -f nexus-lab
```
- `docker volume create` crea el volumen de datos.
- `docker run` crea y arranca un contenedor desde la imagen `sonatype/nexus3`. Flags: `-d` (*detached*, segundo plano), `--name` (nombre), `-p host:contenedor` (publica un puerto: `8091:8081` = tu `localhost:8091` entra al 8081 del contenedor; `8092:8092` lo usarás para el registro Docker en la Fase 9), `-v volumen:ruta` (monta el volumen).
- `docker logs -f` sigue el log en vivo (`-f` = *follow*). Tarda **2 a 3 minutos**. Cuando veas `Started Sonatype Nexus` está listo. Sal con `Ctrl + C` (no lo apaga).
- Necesita memoria: ~4 GB para Docker/WSL es lo cómodo. Con menos puede reiniciarse solo (`docker ps -a` mostraría `Exited`).

### Paso 2 — Entra por primera vez
```powershell
docker exec nexus-lab cat /nexus-data/admin.password
```
- `docker exec contenedor comando` ejecuta un comando **dentro** del contenedor (aquí lee la contraseña inicial).
Abre `http://localhost:8091` → **Sign in** con usuario `admin` y esa contraseña → el asistente te pide una contraseña nueva y decidir el acceso anónimo (**desactívalo**: que nadie descargue sin cuenta).
Nexus *Community Edition* muestra un "Usage Center" con límites (componentes y peticiones por día); para tu práctica sobran.

### Paso 3 — Recorre la interfaz
- **Browse**: lista los repositorios (`maven-central`, `maven-public`, `maven-releases`, `maven-snapshots`). Ábrelos: aún vacíos.
- Engrane → **Repositories**: mira el tipo (hosted/proxy/group) y en `maven-releases` el campo **Deployment policy** (*Disable redeploy*).
- Engrane → **Security → Users → Create local user**: ID `deployer`, una contraseña, y rol `nx-admin` (para el laboratorio). *En un entorno real se crea un rol con solo los privilegios de publicar en esos repos (mínimo privilegio).*

### Paso 4 — `settings.xml` (tus credenciales, fuera del repo)
```powershell
New-Item -ItemType File -Force "$env:USERPROFILE\.m2\settings.xml"
notepad "$env:USERPROFILE\.m2\settings.xml"
```
- `New-Item -ItemType File -Force ruta` crea el archivo (y las carpetas que falten).
- Pega el contenido de `docs\plantillas\settings.xml.ejemplo` con tu usuario y contraseña de Nexus. Guarda. Verifica que no se llame `settings.xml.txt`.

### Paso 5 — `distributionManagement` en el pom padre
```powershell
git switch main
git pull
git switch -c feature/nexus
```
Abre `docs\plantillas\pom-distribucion.xml.ejemplo`, copia el bloque `<distributionManagement>` en el `pom.xml` **de la raíz** (justo donde está el comentario de la Fase 8) y revisa que los puertos sean `8091`.
- Los módulos lo heredan.
- Los `<id>` (`nexus-releases`, `nexus-snapshots`) deben ser iguales a los de tu `settings.xml`.

### Paso 6 — Publica
```powershell
mvn -B deploy
```
Resultado esperado: al final de cada módulo verás líneas `Uploading to nexus-snapshots: http://localhost:8091/repository/maven-snapshots/...` y `BUILD SUCCESS`.
Se suben **tres cosas**: el POM padre, `parqueo-api` y `avisos` (con sus `.pom`, `.jar` y archivos de huella).
En Nexus → **Browse → maven-snapshots**: navega `mx/itson/devops/parqueo-api/1.0.0-SNAPSHOT/`. Verás:
- `parqueo-api-1.0.0-20xxxxxx.xxxxxx-1.jar` ← marca de tiempo y número de build.
- `maven-metadata.xml` ← índice que Maven usa para resolver "el último snapshot".
- `.sha1` / `.md5` ← **huellas** para comprobar que el archivo no se corrompió.
Haz clic en el `.jar` y revisa *Coordinates*, *Uploader*, *Size*. **Esa es la captura 1 del Avance 3.**
Publica una segunda vez (`mvn -B deploy`): se agrega otro archivo con otra marca de tiempo. Los SNAPSHOT sí se repiten.

### Paso 7 — Mira la diferencia con `install`
```powershell
mvn -B install -DskipTests
dir "$env:USERPROFILE\.m2\repository\mx\itson\devops\parqueo-api"
```
`-DskipTests` omite las pruebas (decisión consciente aquí; en CI ya corrieron). Es el repositorio **local**: solo tú lo ves. Nexus es el **compartido**.

### Paso 8 — Laboratorio de fallas (cada uno con su causa)
Provoca, lee el mensaje completo y arréglalo:

| Falla | Cómo provocarla | Qué verás | Causa |
|---|---|---|---|
| **401 Unauthorized** | contraseña mal en `settings.xml` | `status code: 401, reason phrase: Unauthorized` | credenciales incorrectas |
| **401 sin credenciales** | cambia un `<id>` del settings (`nexus-snapshotz`) | el mismo 401 | no coincide con el `<id>` del pom, Maven no envía credenciales |
| **403 Forbidden** | usuario sin privilegios de publicar | `403` | autenticó pero no tiene permiso |
| **405 Method Not Allowed** | apunta la URL a `.../repository/maven-public/` | `405` | un *group* no acepta subidas |
| **400 Bad Request** (redeploy) | pasa a RELEASE y publica **dos veces** | `Repository does not allow updating assets: maven-releases` | las releases son inmutables |
| **Connection refused** | apaga Nexus (`docker stop nexus-lab`) y publica | `Connection refused` | el servidor no está corriendo |

Para la prueba de RELEASE (rama de práctica):
```powershell
mvn -B versions:set -DnewVersion=1.0.0 -DgenerateBackupPoms=false
mvn -B deploy -DskipTests
mvn -B deploy -DskipTests
```
- `versions:set` cambia la versión en el padre y en los módulos de una sola vez (`-DgenerateBackupPoms=false` evita archivos `pom.xml.versionsBackup`).
- La 1.ª publicación pasa y llega a `maven-releases`; la 2.ª falla con 400. Regresa a SNAPSHOT:
```powershell
mvn -B versions:set -DnewVersion=1.0.0-SNAPSHOT -DgenerateBackupPoms=false
```

### Paso 9 — Cierra bien
```powershell
git add pom.xml
git commit -m "build: distributionManagement hacia Nexus"
git push -u origin feature/nexus
```
(Verifica con `git status` que **no** aparece `settings.xml` ni nada con credenciales.) Abre PR, espera el CI verde y mergea.

Para apagar y volver a encender Nexus sin perder nada:
```powershell
docker stop --time=120 nexus-lab
docker start nexus-lab
```
- `--time=120` le da hasta 120 s para cerrar sus bases de datos de forma ordenada.

### Paso 10 — Conecta con el proyecto en equipo
El Nexus del equipo en AWS funciona **igual**: solo cambian la URL (`http://aws-rojo.rinarasoft.com:8081/...`) y las credenciales. Lo que aprendiste aquí es exactamente lo que se repite en la demo.

---

## 3. Evidencia a guardar
- [ ] Captura del `.jar` en `maven-snapshots` (Coordinates + timestamp).
- [ ] Salida de `mvn -B deploy` con `Uploading to nexus-snapshots`.
- [ ] Tabla del laboratorio de fallas completada con tus mensajes reales.
- [ ] Tu justificación: ¿SNAPSHOT o RELEASE? ¿por qué?

## 4. Para entender de verdad
1. ¿Qué une al `pom.xml` con el `settings.xml`?
2. ¿Por qué un `group` no acepta subidas?
3. ¿Qué pasa con tus datos de Nexus si haces `docker rm nexus-lab`? ¿Y si además borras el volumen?
4. ¿Por qué las releases son inmutables y qué problema evita eso?
5. ¿Qué es la huella `.sha1` y para qué sirve?
