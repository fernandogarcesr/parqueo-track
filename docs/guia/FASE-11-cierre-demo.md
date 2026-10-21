# FASE 11 — Cierre: evidencia, bitácora y demo en vivo

**Qué vas a lograr:** juntar todo lo que hiciste en un solo relato demostrable, ensayar la demo en vivo (como la de Avance 3) y comprobar que entiendes cada pieza.

---

## 1. La historia completa (el "delivery system")

```
código ─► git (ramas, PR) ─► GitHub Actions ─► mvn verify (compila + pruebas)
                                   │                     │
                                   │                     ├─► JaCoCo (cobertura) ─► SonarQube Cloud (Quality Gate)
                                   │                     └─► .jar  ──► mvn deploy ──► Nexus (maven-snapshots / releases)
                                   └─► docker build ─► imagen ─► docker push ─► Nexus (registro Docker)
                                                                        └─► docker pull/run en otro lugar (laptop / EC2)
```
Cada flecha es algo que ya hiciste con tus manos en las Fases 0–10. La pregunta del curso es siempre: **¿qué evidencia tengo de que esta flecha funciona y qué pasa si falla?**

## 2. Tabla de evidencia (llénala)

| Fase | Qué demuestra | Evidencia (captura / link / salida) | ✔ |
|---|---|---|---|
| 0 | Repo, primer push, CI en rojo por una razón real | | |
| 1 | Build sin IDE; lees un error de compilación | | |
| 2 | Workflow, jobs, steps, artefacto de reportes | | |
| 3 | Rama → PR → rojo → verde; conflicto resuelto; ADR | | |
| 4 | Pruebas AAA / assertThrows, bugs encontrados por pruebas | | |
| 5 | Parametrizadas, TDD, límites | | |
| 6 | stash, amend, add -p, revert | | |
| 7 | Quality Gate rojo → verde, rama protegida | | |
| 8 | `.jar` en Nexus, fallas 401/405/400 | | |
| 9 | Imagen en Nexus, pull y run desde registro | | |
| 10 | App corriendo en EC2, instancia terminada | | |

## 3. Bitácora de comandos
Completa `docs/plantillas/bitacora-comandos.md` con **tus** palabras: para cada comando, qué hace, por qué se usa y qué error evita. Si no puedes explicarlo sin mirar, vuelve a esa fase.

## 4. Guion de demo en vivo (≈ 8 minutos)

Prepara antes: Docker Desktop abierto, Nexus (`docker start nexus-lab`) corriendo, ventana de la terminal en la carpeta del proyecto, navegador con GitHub, Nexus y SonarQube Cloud.

1. **Contexto (30 s):** "Esto es ParqueoTrack: calcula cobros de estacionamiento. El objetivo es mostrar el camino de un cambio hasta un artefacto ejecutable."
2. **Rama y cambio pequeño:** `git switch -c demo/cambio` · edita un comentario o una prueba · commit · push · PR.
3. **CI:** muestra el run en Actions: jobs, steps, ✅. Explica qué corrió (`mvn -B verify`).
4. **Calidad:** muestra el Quality Gate en SonarQube Cloud y la protección de rama (botón de merge bloqueado si algo falla).
5. **Artefacto:** `mvn -B deploy` → muestra en Nexus el `.jar` (coordenadas, uploader, marca de tiempo).
6. **Imagen:** `docker build -t localhost:8092/parqueo/parqueo-api:demo .` → `docker push` → muestra la imagen en Nexus.
7. **Prueba de que es reproducible:** `docker rmi` la imagen local → `docker pull` → `docker run -d -p 8080:8080 ...` → `curl.exe http://localhost:8080/health`.
8. **Cierre:** una frase sobre qué falló alguna vez y cómo lo diagnosticaste (los errores 401/405/400, el Quality Gate rojo...). Mostrar **un error y su diagnóstico** vale más que una demo sin fallas.

**Plan B** (si algo falla en vivo): ten capturas de cada paso y di exactamente qué error aparece y cuál es su causa. Eso también es evidencia de que entiendes.

## 5. Cómo se traduce a OrderFlow (tu proyecto en equipo)
| En ParqueoTrack | En OrderFlow |
|---|---|
| `distributionManagement` hacia `localhost:8091` | hacia `aws-rojo.rinarasoft.com:8081` |
| Credenciales en tu `settings.xml` | las credenciales que te dé tu equipo/maestro, en tu `settings.xml` |
| Nexus en tu laptop | Nexus en la EC2 del maestro (≥ 4 GB RAM) |
| Registro Docker `localhost:8092` | el puerto de registro Docker que defina el equipo (confírmalo) |
| Tag de imagen `1.0.0` / SHA | `orderflow:${{ github.sha }}` en el CI |
Preguntas que debes resolver con tu equipo: ¿qué puerto usa el registro Docker?, ¿qué usuario/rol usa el CI?, ¿la demo usa el Nexus del servidor o el local?

## 6. Preguntas finales (responde sin mirar)
1. Explica con tus palabras la diferencia entre CI, entrega y despliegue.
2. ¿Qué pasa si `mvn verify` pasa en tu laptop pero falla en el runner? Da dos causas.
3. ¿Qué dos archivos conectan Maven con Nexus y qué los une?
4. ¿Por qué la imagen se etiqueta con versión o SHA y no solo `latest`?
5. ¿Qué diferencia hay entre una imagen y un contenedor?
6. ¿Por qué SonarQube necesita `fetch-depth: 0`?
7. ¿Por qué un secreto subido por error se *rota* y no basta con borrarlo?
8. ¿Qué cobra AWS aunque la instancia esté detenida?
