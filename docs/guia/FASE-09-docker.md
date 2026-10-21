# FASE 9 — Docker: Dockerfile multi-stage, registro en Nexus y CI

**Qué vas a lograr:** empaquetar ParqueoTrack en una imagen Docker, versionarla, publicarla en el registro Docker de tu Nexus, correrla **desde el registro** (no desde tu copia local) y construirla también en CI con el SHA del commit.
**Curso:** Semana 6 (miércoles) · Avance 3, segunda mitad.

---

## 1. Conceptos

### "Funciona en mi máquina" no es un argumento de ingeniería
Tu laptop tiene todo instalado a mano durante meses (versión de Java, variables de entorno, rutas). Una **imagen** empaqueta el runtime exacto: si corre en la imagen, corre igual en cualquier lugar que la ejecute.
Por eso Docker no es "más rápido de desplegar": es la forma de garantizar **el mismo entorno en todos lados**.

### Imagen, contenedor y registro
| Concepto | Qué es | Analogía |
|---|---|---|
| **Imagen** | plantilla **inmutable** (código + runtime + dependencias + configuración) en capas; se construye una vez y se usa muchas | la clase |
| **Contenedor** | una **instancia en ejecución** de una imagen; puedes correr varios a la vez | el objeto |
| **Registro (registry)** | almacén de imágenes (Docker Hub, Nexus...) | un repositorio de imágenes |
Ya lo viviste: `sonatype/nexus3` es la IMAGEN; `nexus-lab` es el CONTENEDOR.

### El Dockerfile y sus instrucciones
Un **Dockerfile** es la receta para construir una imagen. Cada instrucción crea una **capa** (Docker las cachea: si no cambió, la reutiliza).
| Instrucción | Qué hace |
|---|---|
| `FROM imagen` | imagen base de la que partes |
| `AS build` | le pone nombre a una etapa para referirla después |
| `WORKDIR /app` | carpeta de trabajo dentro de la imagen |
| `COPY origen destino` | copia archivos de tu proyecto a la imagen |
| `RUN comando` | ejecuta un comando **al construir** (aquí `mvn package`) |
| `EXPOSE 8080` | **documenta** el puerto (no lo abre) |
| `ENTRYPOINT [...]` | el comando que corre el contenedor **al arrancar** (forma `exec`: lista) |
| `CMD` | argumentos/valores por omisión que se pueden sobrescribir (no lo usamos aquí) |

### ¿Por qué multi-stage?
- **Una sola etapa**: `FROM maven:...` para todo → la imagen final arrastra Maven, el SDK completo y el código fuente (fácil 500+ MB).
- **Multi-stage**: etapa 1 (`build`) compila con Maven y JDK; etapa 2 (`runtime`) copia **solo el `.jar`** a una imagen mínima con JRE. Nada de SDK ni código fuente.
- Resultado: imagen más chica, más rápida de transferir y con **menos superficie de ataque** (menos herramientas que un intruso podría usar).
- `COPY --from=build` trae **solo lo que pides explícitamente** de la etapa anterior.
- `-jre` = solo lo necesario para ejecutar; `-alpine` = Linux mínimo. `eclipse-temurin:21` (sin `-jre`) traería el JDK completo.
- `-DskipTests` en el Dockerfile es una **decisión consciente**: las pruebas ya corrieron en CI antes de llegar aquí; no se duplica el trabajo.

### `.dockerignore`
Como `.gitignore`, pero para `docker build`: lo que lista **no se envía** al contexto de build (`target/`, `.git`, llaves `.pem`...). Build más rápido y sin basura ni secretos dentro de la imagen.

### Nombre completo de una imagen
```
localhost:8092 / parqueo / parqueo-api : 1.0.0
   ↑ registro      ↑ grupo    ↑ nombre     ↑ tag (versión)
```
Si no pones registro, Docker asume Docker Hub. **El nombre lleva la dirección del registro**: así `docker push` sabe a dónde subir.

### Tags: por qué no solo `latest`
`latest` no es una versión: es un alias que cambia de significado cada vez que alguien publica. Con `latest` pierdes **trazabilidad** (¿qué código generó lo que corre?). Usa tag de **versión** (`1.0.0`) y, en CI, el **SHA del commit**.

### Nexus también es registro Docker
Un repositorio `docker (hosted)` necesita **su propio puerto HTTP** (aquí 8092), porque el protocolo del registro (*Docker Registry API v2*) es distinto al HTTP de Maven. Además hay que activar el *Docker Bearer Token Realm* o `docker login` falla.

---

## 2. Pasos

### Paso 1 — Dockerfile y `.dockerignore`
```powershell
git switch main
git pull
git switch -c feature/docker
Copy-Item docs\plantillas\Dockerfile Dockerfile
Copy-Item docs\plantillas\dockerignore.txt .dockerignore
```
- Abre `Dockerfile` y léelo línea por línea con la tabla de arriba. Pregunta: ¿qué haría `COPY . .` sin `.dockerignore`?
- `-pl parqueo-api -am`: `-pl` (*projects list*) = solo ese módulo; `-am` (*also make*) = también lo que necesita (el POM padre).

### Paso 2 — Construye y corre
Docker Desktop debe estar en "Engine running".
```powershell
docker build -t parqueo-api:1.0.0 .
docker images
docker run --rm -p 8080:8080 parqueo-api:1.0.0
```
- `docker build -t nombre:tag .` construye la imagen. `-t` = *tag* (nombre:versión). El `.` final es el **contexto**: la carpeta que se envía a Docker.
  - La 1.ª vez descarga las imágenes base (`maven:...`, `eclipse-temurin:...`) y tarda varios minutos. Observa cómo cada instrucción es un paso (`[build 3/5]`...). Si repites el build sin cambios, usa la **caché** y termina en segundos.
- `docker images` lista imágenes con su **SIZE**: anota el de `parqueo-api:1.0.0`.
- `docker run --rm -p 8080:8080 imagen`: `--rm` borra el contenedor al detenerse; `-p 8080:8080` = `puerto-de-tu-compu:puerto-del-contenedor`.
Prueba desde **otra terminal** (con `curl.exe`):
```powershell
curl.exe http://localhost:8080/health
curl.exe "http://localhost:8080/api/cobro?minutos=61&pase=ITSON20"
```
La segunda es tu **prueba de extremo a extremo**: si ya corregiste las Fases 3 y 4 debe responder `"total":14`; si aún tienes defectos (por ejemplo el `==` de `PaseDescuento`) responderá 400 y ahora sabes por qué. Detén con `Ctrl + C`.

### Paso 3 — Variables de entorno
```powershell
docker run --rm -e PORT=9090 -p 9090:9090 parqueo-api:1.0.0
```
- `-e NOMBRE=valor` define una variable de entorno dentro del contenedor. `Main` lee `PORT`. La **misma imagen** corre distinto según la configuración; no hay que reconstruir.

### Paso 4 — Compara con una sola etapa
```powershell
docker build -f docs/plantillas/Dockerfile.una-etapa -t parqueo-api:una-etapa .
docker images
```
- `-f archivo` usa otro Dockerfile. Compara los SIZE: la de una etapa pesa mucho más. Anota ambos números: es el argumento visual del Avance 3 ("tamaño de la imagen").
- Mira las capas: `docker history parqueo-api:1.0.0`.

### Paso 5 — Crea el repositorio Docker en Nexus
(Asegúrate de que `nexus-lab` esté arriba: `docker start nexus-lab`.)
1. Nexus (`http://localhost:8091`) → engrane → **Repositories → Create repository → docker (hosted)**.
2. Name: `docker-lab`. **HTTP**: marca y escribe el puerto **8092**. Deployment policy: *Allow redeploy* (para el laboratorio).
3. **Create repository**.
4. Engrane → **Security → Realms** → mueve **Docker Bearer Token Realm** a *Active* → **Save**.
El puerto 8092 ya lo publicaste al crear el contenedor (`-p 8092:8092`).
> Si olvidaste mapearlo, hay que recrear el contenedor con el mismo volumen (los datos no se pierden): `docker stop nexus-lab`, `docker rm nexus-lab`, y el `docker run` de la Fase 8 con los dos `-p`.

### Paso 6 — Login, tag y push
```powershell
docker login localhost:8092 -u deployer
docker tag parqueo-api:1.0.0 localhost:8092/parqueo/parqueo-api:1.0.0
docker push localhost:8092/parqueo/parqueo-api:1.0.0
```
- `docker login servidor -u usuario` te pide la contraseña (la de tu usuario de Nexus). El mensaje "password stored unencrypted" es solo un aviso.
- `docker tag origen destino` **no copia nada**: crea otro nombre apuntando a la misma imagen. Aquí le agregas la dirección del registro.
- `docker push` sube las capas (solo las que el registro no tiene).
En Nexus → **Browse → docker-lab**: aparece `parqueo/parqueo-api` con el tag `1.0.0`. **Esa es la captura 2.**

### Paso 7 — Corre desde el registro (la prueba que pide el maestro)
```powershell
docker rmi localhost:8092/parqueo/parqueo-api:1.0.0
docker rmi parqueo-api:1.0.0
docker images
docker pull localhost:8092/parqueo/parqueo-api:1.0.0
docker run -d --name parqueo -p 8080:8080 localhost:8092/parqueo/parqueo-api:1.0.0
curl.exe http://localhost:8080/health
docker ps
docker logs parqueo
docker images
```
- `docker rmi imagen` **borra la imagen local** (primero la etiqueta del registro y luego la original), y `docker images` lo confirma: ya no hay copia local.
- `docker pull` la **descarga desde Nexus** (verás las capas "Pull complete" y un `Digest: sha256:...`: es la huella de la imagen).
- `docker run -d --name parqueo ...` la arranca en segundo plano con nombre.
- `docker ps` lista contenedores en ejecución; `docker logs parqueo` muestra su salida ("ParqueoTrack escuchando...").
- Por último `docker images` (**captura 3**: la imagen con su tag y el SIZE).
Limpieza:
```powershell
docker stop parqueo
docker rm parqueo
```

### Paso 8 — Tag con el SHA del commit (trazabilidad)
```powershell
$sha = git rev-parse --short HEAD
docker tag parqueo-api:1.0.0 localhost:8092/parqueo/parqueo-api:$sha
docker push localhost:8092/parqueo/parqueo-api:$sha
```
- `git rev-parse --short HEAD` imprime el hash corto del commit actual; `$sha = ...` lo guarda en una variable de PowerShell.
- Ahora la misma imagen tiene dos tags: la **versión** (humana) y el **commit** (exacto). Búscalos en Nexus.

### Paso 9 — Agrega la imagen al pipeline de CI
Abre `docs\plantillas\ci-paso-docker.yml.ejemplo` y pega el paso **al final** de los `steps` de `.github/workflows/ci.yml` (misma sangría que los demás).
```powershell
git add Dockerfile .dockerignore .github
git commit -m "ci: construir imagen Docker con el SHA del commit como tag"
git push -u origin feature/docker
```
Abre el PR. En Actions, abre el paso **Construir imagen Docker** y mira el `docker build`.
- `${{ github.sha }}` es el hash del commit que disparó el workflow: cada build de CI queda ligado a un commit sin que nadie tenga que subir un número de versión.
- **¿Por qué solo `build` y no `push`?** El Nexus corre en tu laptop (`localhost`); el runner de GitHub vive en la nube y no pueden verse. Para que CI publicara tendrías que exponer Nexus a Internet (credenciales en *secrets*, seguridad...), algo fuera del alcance. Lo que sí logras: verificar en cada commit que el **Dockerfile es válido** y que la imagen se construye.

### Paso 10 — Higiene de disco
```powershell
docker system df
docker ps -a
```
- `docker system df` muestra cuánto ocupan imágenes, contenedores y volúmenes. En un servidor con poco disco (como el del equipo) esto importa.
- Limpiar lo que no usas: `docker image prune` (imágenes sin uso). ⚠️ Revisa **antes** qué borrarías; **nunca** borres el volumen de datos de Nexus.

---

## 3. Evidencia a guardar
- [ ] SIZE de la imagen multi-stage vs una sola etapa (`docker images`).
- [ ] Captura de la imagen con tag en `docker-lab` (Nexus).
- [ ] Salida de `docker rmi` → `docker pull` → `docker run` → `curl.exe /health`.
- [ ] Link al run de CI con el paso `Construir imagen Docker`.

## 4. Errores comunes
| Síntoma | Causa | Arreglo |
|---|---|---|
| `http: server gave HTTP response to HTTPS client` | el registro habla HTTP | Docker Desktop → Settings → Docker Engine → agrega `"insecure-registries": ["localhost:8092"]` → *Apply & restart* |
| `unauthorized: authentication required` / login falla | falta el *Docker Bearer Token Realm* o credenciales mal | actívalo en Security → Realms; revisa usuario/contraseña |
| `connection refused` a `:8092` | no publicaste el puerto o no creaste el repo con ese puerto | `docker ps` (¿aparece `8092->8092`?) y revisa el HTTP port del repo |
| `port is already allocated` | otro programa/contenedor usa 8080 | detén el otro o usa `-p 8090:8080` |
| `Conflict. The container name "/parqueo" is already in use` | ya existe un contenedor con ese nombre | `docker rm parqueo` |
| `no space left on device` | disco de Docker lleno | `docker system df` y limpia imágenes sin uso |
| `COPY failed: no source files` | cambió el nombre del jar o no se construyó | revisa el log de la etapa `build` |

## 5. Para entender de verdad
1. ¿Qué diferencia hay entre una imagen y un contenedor?
2. ¿Por qué `COPY --from=build` solo trae el `.jar`?
3. ¿Qué pasa si la base de runtime fuera `eclipse-temurin:21` en vez de `-jre-alpine`?
4. ¿Por qué el nombre de la imagen incluye `localhost:8092/`?
5. ¿Por qué usar `${{ github.sha }}` como tag en CI en vez de un número fijo?
6. ¿Qué demuestra que el contenedor corre desde el registro y no desde tu copia local?
