# FASE 10 — AWS: tu propia instancia EC2 (gratis y sin sorpresas)

**Qué vas a lograr:** crear una cuenta de AWS sin gastar, lanzar una instancia EC2, entrar por SSH con tu llave `.pem` (igual que en el servidor del maestro), instalar Docker y correr ahí la imagen de ParqueoTrack, controlando costos.
**Curso:** el servidor de equipo (EC2 con Nexus) y la pregunta "¿qué significa desplegar?".

---

## 1. Conceptos

### Cloud, IaaS y EC2
- **AWS** renta infraestructura por uso. **EC2** (*Elastic Compute Cloud*) = **computadoras virtuales** a las que entras por terminal. Una **instancia** es una de esas computadoras.
- El servidor del equipo es exactamente eso: una instancia Ubuntu con IP pública, a la que entras por SSH.

### Piezas de una instancia
| Pieza | Qué es |
|---|---|
| **Región / AZ** | dónde está físicamente (p. ej. `us-east-1`, N. Virginia). Elige una y quédate en ella |
| **AMI** | la "imagen de disco" del sistema operativo (aquí Ubuntu Server LTS) |
| **Tipo de instancia** | vCPU + RAM (p. ej. 1 vCPU / 1 GB). Más RAM = más costo |
| **Par de llaves (key pair)** | llave **pública** (AWS la instala en la instancia) + llave **privada** (el `.pem` que TÚ descargas **una sola vez**) |
| **Security Group** | el **firewall virtual**: reglas de entrada (qué puertos acepta y de qué IPs) |
| **IP pública** | la dirección para llegar a ella (cambia si apagas y enciendes la instancia, salvo que uses una IP elástica) |
| **EBS** | el disco virtual (se cobra por GB mientras exista, aunque la instancia esté apagada) |

### SSH y el archivo `.pem`
- **SSH** abre una terminal remota cifrada.
- Se autentica con **criptografía de llave pública**: el servidor guarda la parte pública; tú demuestras que tienes la privada **sin enviarla**. Quien tenga el `.pem` puede entrar: **no se sube a GitHub, no se manda en capturas ni chats**.
- Windows/SSH exige que el `.pem` solo sea legible por ti (por eso el comando `icacls`).

### `mvn deploy` ≠ desplegar en AWS
`mvn deploy` publica el **artefacto** en Nexus. "Desplegar" es **ejecutar** la aplicación en un servidor. Aquí harás ambas cosas por separado, para que quede clara la diferencia.

### Costos: lo que sí puede sorprenderte
- **Plan gratuito de AWS (cuentas nuevas)**: según la documentación oficial de AWS, tanto el plan *Free* como el *Paid* empiezan con **US$100 de crédito**, y puedes ganar **hasta US$100 más** completando actividades. El plan *Free*:
  - **no genera cargos** mientras lo uses;
  - ofrece **solo ciertos servicios**;
  - **termina a los 6 meses o cuando se acaban los créditos**, lo que ocurra primero; entonces la cuenta **se cierra**. AWS conserva tu contenido **90 días**; para conservarla debes *subir de plan* (*Upgrade plan*).
  - Fuentes: <https://aws.amazon.com/free/> y <https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html>
- Si tu cuenta es **anterior a julio de 2025**, puede regirse por el esquema anterior (12 meses con horas gratis de ciertas instancias): revisa **Billing → Free Tier** en tu consola.
- **No hay una lista fija que yo pueda garantizarte** de tipos de instancia gratis: al lanzar la instancia, la consola marca cuáles son **elegibles para el plan gratuito**. Elige una de esas; no inventes.
- Cosas que cuestan aparte (el crédito las cubre, pero se gastan): **IP pública** (AWS cobra por IPv4 pública; revisa el precio vigente), **disco EBS** que sigas teniendo, y cualquier instancia más grande de lo necesario.
- **Regla de oro:** al terminar la práctica, **termina (terminate) la instancia** y verifica en Billing que no queda nada corriendo.

### ¿Y Nexus en AWS?
Nexus necesita memoria: tu equipo vio que con **1.9 GB de RAM** el contenedor se caía por falta de memoria (el maestro lo subió a 4 GB) y que **6.7 GB de disco al 99 %** también rompe todo. Por eso en esta práctica:
- Nexus de práctica → **en tu laptop** (Fases 8 y 9).
- EC2 de práctica → una instancia **pequeña** que solo **ejecuta** tu app.
- (Opcional 10B al final: Nexus en EC2, si decides gastar créditos en una instancia de ≥ 4 GB.)

---

## 2. Pasos

### Paso 1 — Cuenta de AWS
1. Entra a **aws.amazon.com/free** → *Create a Free Account*.
2. Correo, nombre de cuenta, verificación y una tarjeta (la pide para verificar identidad). Elige el **plan Free** cuando te lo ofrezca.
3. Protege la cuenta raíz con **MFA** (Consola → tu nombre → *Security credentials* → *Assign MFA device*). La cuenta raíz tiene poder total: en un entorno real no se usa para el trabajo diario.

### Paso 2 — Alarma de gasto (antes de crear nada)
Consola → **Billing and Cost Management → Budgets → Create budget** → plantilla **Zero spend budget** (te avisa por correo si algo empieza a generar costo). Así ningún olvido te cuesta dinero sin enterarte.

### Paso 3 — Lanza la instancia
Consola → **EC2** → arriba a la derecha elige tu región (por ejemplo **N. Virginia, us-east-1**) → **Launch instance**:
- **Name:** `parqueo-lab`
- **AMI:** *Ubuntu Server* (LTS) — la que diga **Free tier eligible** si aparece la etiqueta.
- **Instance type:** uno marcado como elegible para el plan gratuito.
- **Key pair → Create new key pair:** nombre `parqueo-lab-key`, tipo **RSA**, formato **.pem** → se descarga **una sola vez**. Guárdalo en `C:\Users\TU_USUARIO\.ssh\` (y nunca dentro del repo).
- **Network settings → Edit:** *Create security group*; regla **SSH (puerto 22)** con origen **My IP** (no `0.0.0.0/0`: eso abre tu servidor a todo Internet).
- **Storage:** el valor por omisión (≈ 8 GiB) basta.
- **Launch instance**. En la lista de instancias espera `Running` y copia la **Public IPv4 address**.

### Paso 4 — Entra por SSH (igual que en la instancia del maestro)
PowerShell:
```powershell
icacls "C:\Users\TU_USUARIO\.ssh\parqueo-lab-key.pem" /inheritance:r /grant:r "${env:USERNAME}:(R)"
ssh -i "C:\Users\TU_USUARIO\.ssh\parqueo-lab-key.pem" ubuntu@IP_PUBLICA
```
- `icacls ... /inheritance:r /grant:r "usuario:(R)"` deja el archivo legible **solo para ti** (`/inheritance:r` quita permisos heredados, `/grant:r` da lectura). Sin esto SSH dice *UNPROTECTED PRIVATE KEY FILE*.
- `ssh -i llave usuario@ip`: `-i` = archivo de identidad (el `.pem`); `ubuntu` es el usuario por omisión de las AMI de Ubuntu (en Amazon Linux sería `ec2-user`).
- La primera vez pregunta si confías en la huella del servidor: escribe `yes`.
- Prompt `ubuntu@ip-...:~$` = estás **dentro** de la instancia. `exit` para salir.

### Paso 5 — Reconoce el servidor
```bash
whoami
free -h
df -h /
uname -a
```
- `free -h` = memoria (RAM total, usada, libre). `df -h /` = espacio en disco del sistema de archivos raíz. `uname -a` = versión del kernel.
- Estos son los mismos comandos que sirven para diagnosticar lo que le pasó al servidor de tu equipo (memoria llena y disco al 99 %).

### Paso 6 — Instala Docker en la instancia
```bash
sudo apt-get update
sudo apt-get install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
exit
```
- `sudo` ejecuta como administrador. `apt-get update` refresca la lista de paquetes; `apt-get install -y docker.io` instala Docker (`-y` = responder "sí" solo).
- `systemctl enable --now docker` arranca el servicio ahora y en cada reinicio.
- `usermod -aG docker ubuntu` agrega tu usuario al grupo `docker` (para no escribir `sudo` en cada comando). Hay que **salir y volver a entrar** para que aplique.
Vuelve a entrar con `ssh` y verifica:
```bash
docker --version
docker ps
```

### Paso 7 — Lleva tu imagen a la instancia (sin registro)
Desde **tu laptop** (otra terminal):
```powershell
docker save -o parqueo-api.tar parqueo-api:1.0.0
scp -i "C:\Users\TU_USUARIO\.ssh\parqueo-lab-key.pem" parqueo-api.tar ubuntu@IP_PUBLICA:~/
```
- `docker save -o archivo imagen` empaqueta la imagen en un `.tar`.
- `scp -i llave archivo usuario@ip:ruta` copia por SSH (*secure copy*). `~/` = tu carpeta personal en el servidor.
Ya en la instancia:
```bash
docker load -i parqueo-api.tar
docker images
```
- `docker load -i archivo` importa la imagen.
> ¿Por qué no construir en la instancia? Un build de Maven necesita memoria y la instancia pequeña puede quedarse sin RAM (justo lo que le pasó al servidor del equipo). Construir una vez y **mover la imagen** es la idea de Docker: *la imagen es lo que viaja*.

### Paso 8 — Córrela y ábrela al mundo (con cuidado)
```bash
docker run -d --name parqueo --restart unless-stopped -p 8080:8080 parqueo-api:1.0.0
docker ps
docker logs parqueo
```
- `--restart unless-stopped` la vuelve a levantar si la instancia se reinicia.
Ahora abre el puerto en el **Security Group**: EC2 → tu instancia → *Security* → el security group → *Edit inbound rules* → **Add rule**: tipo *Custom TCP*, puerto **8080**, origen **My IP**.
Desde tu laptop:
```powershell
curl.exe http://IP_PUBLICA:8080/health
curl.exe "http://IP_PUBLICA:8080/api/cobro?minutos=61&pase=ITSON20"
```
Si responde, **desplegaste**: la misma imagen que corrió en tu laptop corre en un servidor remoto, sin instalar Java ni Maven allí.

### Paso 9 — Experimentos que enseñan
1. **La IP cambia:** detén la instancia (*Instance state → Stop*) y vuelve a iniciarla: la IP pública cambia. Conclusión: por eso las URLs del `pom.xml` apuntan a un **nombre** (como `aws-rojo.rinarasoft.com`) y no a una IP.
2. **El firewall manda:** quita la regla del 8080 y repite el `curl`: deja de responder (tiempo agotado). Es el Security Group, no tu app.
3. **Memoria:** `docker stats --no-stream` muestra cuánta RAM usa tu contenedor.
4. **Bonus (síntoma de OOM):** `docker inspect parqueo --format "{{.State.OOMKilled}}"` dice `true` si Linux mató el contenedor por falta de memoria.

### Paso 10 — Limpieza obligatoria
1. EC2 → **Instances** → selecciona `parqueo-lab` → **Instance state → Terminate instance**.
   - *Stop* apaga pero **sigue cobrando disco**; *Terminate* la destruye.
2. EC2 → **Volumes**: confirma que no quedó ningún volumen.
3. EC2 → **Elastic IPs**: si creaste alguna, *Release*.
4. **Billing** → confirma que no hay recursos activos ni costos.
5. Borra de tu laptop el `.tar` si ya no lo usas (`Remove-Item parqueo-api.tar`).

---

## 3. Opcional 10B — Nexus en EC2
Solo si decides gastar créditos en una instancia de **al menos 4 GB de RAM** (un tipo elegible; verifica en la consola o en el calculador de precios):
- Instala Docker igual que arriba y crea un volumen: `docker volume create nexus-data`.
- Arranca Nexus: `docker run -d --name nexus -p 8081:8081 -p 8082:8082 -v nexus-data:/nexus-data sonatype/nexus3`.
  - Nexus se configura con la variable `INSTALL4J_ADD_VM_PARAMS` (heap y memoria directa de la JVM; el ejemplo de la documentación de la imagen usa `-Xms2703m -Xmx2703m -XX:MaxDirectMemorySize=2703m`). Ajusta esos valores a la RAM que tengas y deja margen para el sistema.
- En el Security Group abre **8081 y 8082 solo para "My IP"**. **Nunca** expongas la administración de Nexus a `0.0.0.0/0`.
- En tu laptop: cambia las URLs del pom a `http://IP_PUBLICA:8081/...`, agrega `IP_PUBLICA:8082` a *insecure registries* de Docker Desktop (el registro habla HTTP) y repite las Fases 8 y 9 contra ese Nexus.
- Vigila **disco** (`df -h /`, `docker system df`) y **memoria** (`free -h`): son las dos fallas que viste en el servidor de tu equipo.
- Esto no cambia el hecho de que **GitHub Actions** haría falta autenticarse con *secrets* para publicar ahí: otra decisión de seguridad (qué exponer a Internet y a quién).

---

## 4. Evidencia a guardar
- [ ] Captura de la instancia `Running` (sin mostrar tu llave) y de la salida de `free -h` / `df -h /`.
- [ ] `curl.exe http://IP_PUBLICA:8080/health` respondiendo `{"status":"UP"}`.
- [ ] Captura del Security Group con la regla 8080 → *My IP*.
- [ ] Captura de la instancia en estado *Terminated* y de Billing sin cargos.

## 5. Errores comunes
| Síntoma | Causa | Arreglo |
|---|---|---|
| `UNPROTECTED PRIVATE KEY FILE` | permisos del `.pem` | repite `icacls` del Paso 4 |
| `Permission denied (publickey)` | usuario o llave equivocados | usuario `ubuntu`; revisa la ruta del `.pem` |
| `Connection timed out` al hacer ssh | el Security Group no permite tu IP / tu IP cambió / red que bloquea el 22 | actualiza la regla SSH a "My IP"; prueba con hotspot |
| `curl` al 8080 se queda colgado | falta la regla de entrada 8080 | agrégala (Paso 8) |
| `docker: permission denied` | el grupo `docker` aún no aplica | `exit` y entra de nuevo |
| `no space left on device` | disco lleno | `df -h /`, `docker system df`, `docker image prune` |
| El contenedor se reinicia o muere | poca RAM | `free -h`, `docker inspect ... OOMKilled`, instancia más grande |

## 6. Para entender de verdad
1. ¿Qué diferencia hay entre `mvn deploy`, `docker push` y `docker run` en el servidor?
2. ¿Para qué sirve el `.pem` y por qué no se comparte?
3. ¿Qué hace un Security Group y por qué SSH solo desde "My IP"?
4. ¿Por qué la IP pública cambia y qué implica para el `pom.xml`?
5. ¿Qué diferencia hay entre *stop* y *terminate*?
