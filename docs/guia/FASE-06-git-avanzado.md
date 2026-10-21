# FASE 6 — Git en la vida real: fetch, pull, stash, conflictos, amend y `git add -p`

**Qué vas a lograr:** dominar los problemas de Git que de verdad aparecen en un equipo: tu `main` desactualizado, cambios a medias que estorban, conflictos, commits mal hechos y PRs revueltos.
**Origen:** situaciones reales de las prácticas y del proyecto en equipo (por ejemplo: hacer `pull` estando en otra rama y luego chocar al recuperar un `stash`).

---

## 1. Conceptos

### Tu repo local y el remoto pueden "desincronizarse"
- `git fetch origin` **descarga** lo nuevo del remoto pero **no toca** tus archivos ni tus ramas (solo actualiza `origin/main`, `origin/otra-rama`...).
- `git pull` = `fetch` + `merge` (o rebase) sobre **la rama en la que estás**. ⚠️ Si estás en `feature/x`, `git pull origin main` mete `main` **dentro de `feature/x`**; tu `main` local sigue viejo.
- Por eso: `git switch main` primero, `git pull` después.
- `git status` te dice "behind by N commits" cuando tu rama va atrás del remoto.

### Mapa de "deshacer" (qué comando según dónde está el error)
| Situación | Comando | Qué hace |
|---|---|---|
| Edité un archivo y quiero **descartar** mis cambios | `git restore archivo` (antes: `git checkout -- archivo`) | vuelve al último commit (⚠️ pierdes los cambios) |
| Hice `git add` y me arrepentí | `git restore --staged archivo` | saca del staging, conserva los cambios |
| Mensaje del último commit mal / olvidé un archivo (**aún sin push**) | `git commit --amend` | reescribe el último commit |
| Ya hice push y hay que deshacer un commit | `git revert <hash>` | crea un commit NUEVO que lo anula (no reescribe historia) |
| Quiero mover la rama a un commit anterior (solo local) | `git reset --hard <hash>` | ⚠️ destructivo |

Regla de seguridad: **no reescribas historia ya publicada** (`amend`, `reset`, `rebase` después de `push`) en ramas que otros usan.

### `git stash`: un cajón temporal
Guarda tus cambios sin commitear para que puedas cambiar de rama con el directorio limpio. Los recuperas luego (`pop`). Si lo que recuperas choca con lo que cambió mientras tanto → **conflicto**.

### Staging selectivo: `git add -p`
`-p` (*patch*) te deja elegir **fragmentos (hunks)** de un archivo para el commit. Sirve cuando mezclaste dos cambios distintos en el mismo archivo y quieres dos commits limpios (un PR legible).

---

## 2. Ejercicios (en el repo del proyecto, en ramas desechables)

### Ejercicio 1 — Estar atrás del remoto
Simula un compañero: en GitHub edita `README.md` (botón del lápiz) y haz commit directo en `main`. Luego, en tu terminal:
```powershell
git switch main
git fetch origin
git status
git log main..origin/main --oneline
git pull
```
- Tras `fetch`, `git status` dice `Your branch is behind 'origin/main' by 1 commit`.
- `git log main..origin/main` = "los commits que están en origin/main y NO en mi main".
- `git pull` hace fast-forward y te pone al día.

### Ejercicio 2 — `stash` (el caso real)
```powershell
git switch -c exp/stash
```
Edita un comentario cualquiera en `Tarifa.java` (no lo commitees) y luego:
```powershell
git switch main
```
Si Git se queja de que tus cambios "se sobrescribirían", es porque `main` toca el mismo archivo. Guarda en el cajón:
```powershell
git stash push -m "comentario en Tarifa" -- parqueo-api/src/main/java/mx/itson/devops/parqueo/Tarifa.java
git stash list
git switch main
git pull
git switch exp/stash
git stash pop
```
- `push -m "texto"` pone una etiqueta. `-- ruta` limita el stash a ese archivo (sin eso guarda todo lo modificado).
- `git stash list` muestra `stash@{0}`, `stash@{1}`...
- `git stash pop` aplica el stash y lo borra de la lista. Si hay conflicto, el stash **se conserva** hasta que lo resuelvas.

### Ejercicio 3 — Provocar y resolver un conflicto al hacer `stash pop`
1. En `exp/stash`, edita la línea de `TOPE_POR_VISITA` en `Tarifa.java` a `90` y haz `git stash push`.
2. Cambia a `main`, edita esa MISMA línea a `100`, `git commit -am "chore: tope temporal"`.
3. `git stash pop` → **CONFLICT**. Abre el archivo y verás `<<<<<<<`, `=======`, `>>>>>>>`.
4. Resuelve (deja el valor correcto, que es **80**, y borra los marcadores).
5. Termina:
   ```powershell
   git restore --staged parqueo-api/src/main/java/mx/itson/devops/parqueo/Tarifa.java
   git stash list
   git stash drop
   ```
   - Tras un `pop` en conflicto el archivo queda en staging: `restore --staged` lo saca.
   - `git stash drop` elimina el stash que ya resolviste.
6. Descarta el commit de prueba: `git reset --hard HEAD~1` (solo porque era local y de práctica).

### Ejercicio 4 — Corregir un commit sin hacer push
```powershell
git switch -c exp/amend
```
Haz un commit con un mensaje con falta de ortografía (`git commit -am "chore: commite de prueba"`) y arréglalo:
```powershell
git commit --amend -m "chore: commit de prueba"
git log --oneline -3
```
El hash **cambia** (es un commit nuevo). Por eso `amend` solo es seguro **antes** del `push`.

### Ejercicio 5 — "PR Forensics": un solo archivo, dos cambios, dos commits
En `Reporte.java` haz a la vez dos cambios no relacionados:
(a) corrige la división `ocupados * 100 / capacidad`, y (b) cambia el nombre del método `etiquetaOcupacion` agregando un comentario de documentación.
```powershell
git diff
git add -p parqueo-api/src/main/java/mx/itson/devops/parqueo/Reporte.java
```
Git te muestra cada fragmento con `Stage this hunk [y,n,q,a,d,s,e,?]?`:
- `y` = sí lo incluyo, `n` = no, `s` = **dividir** el fragmento en otros más pequeños, `e` = editarlo a mano, `q` = salir, `?` = ayuda.
Incluye solo (a), commitea, y después el resto:
```powershell
git commit -m "fix: ocupacion se calcula multiplicando antes de dividir"
git add .
git commit -m "docs: documentar etiquetaOcupacion"
git log --oneline -3
```
- `git diff` (sin argumentos) = cambios no staged; `git diff --staged` = lo que está en staging.
- Resultado: dos commits atómicos que un revisor puede entender (y revertir) por separado.

### Ejercicio 6 — `revert` de algo ya publicado
Haz un commit pequeño en una rama, `git push`, y luego:
```powershell
git revert HEAD --no-edit
git push
```
- `revert` crea un commit que **deshace** el anterior sin borrarlo del historial. `--no-edit` usa el mensaje que Git sugiere.

### Ejercicio 7 — `.gitignore` y un archivo que ya se había subido
Si un archivo que debía ignorarse ya está *tracked*:
```powershell
git rm --cached archivo
git commit -m "chore: dejar de versionar archivo"
```
- `--cached` lo quita del control de versiones **sin borrarlo** de tu disco. (Si fue un secreto, además hay que **rotarlo**: cambiarlo, porque ya quedó en el historial.)

---

## 3. Chuleta de diagnóstico
```powershell
git status                      # dónde estoy y qué cambió
git branch --show-current       # en qué rama estoy
git log --oneline --graph --all -15   # el mapa de ramas
git diff                        # cambios aún no staged
git diff --staged               # lo que va en el próximo commit
git stash list                  # lo que guardé en el cajón
git remote -v                   # a dónde apunta origin
```

## 4. Evidencia a guardar
- [ ] Salida de `git log --graph --oneline --all` de tu conflicto con `stash`.
- [ ] Dos commits creados con `git add -p` a partir de un solo archivo.
- [ ] Respuesta escrita: ¿por qué `git pull` estando en otra rama no actualiza `main`?

## 5. Para entender de verdad
1. ¿Qué diferencia hay entre `fetch` y `pull`?
2. ¿Cuándo es seguro usar `amend` y cuándo no?
3. ¿Por qué `revert` es seguro en una rama compartida y `reset --hard` no?
4. ¿Qué significan los marcadores `<<<<<<<`, `=======` y `>>>>>>>`?
