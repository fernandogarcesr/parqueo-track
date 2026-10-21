# ADR 0001 — Estrategia de branching

> ADR = Architecture Decision Record: un registro corto de UNA decisión, su contexto y sus consecuencias.
> Una respuesta defendible declara: contexto, evidencia y trade-off. (Semana 2 del curso.)

- **Estado:** propuesta
- **Fecha:**

## Contexto
(¿Cuántas personas? ¿Cada cuánto se integra? ¿Hay CI? ¿Se despliega seguido?)

## Opciones consideradas (mínimo 3)
| Estrategia | Ventaja | Costo |
|---|---|---|
| Trunk-Based | Feedback temprano, lotes pequeños | Exige disciplina y automatización |
| GitHub Flow | Simple y compatible con Pull Requests | Se degrada si las ramas viven demasiado |
| GitFlow | Separación explícita release/hotfix | Más ramas, más merges, más divergencia |

## Decisión
(Cuál eliges y por qué, citando evidencia, por ejemplo lo que viste en tu sandbox de conflictos.)

## Consecuencias
- Reglas operativas (ramas cortas, PR obligatorio, nombres de rama...):
- Qué aceptamos perder (el trade-off):
