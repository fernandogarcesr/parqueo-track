# ParqueoTrack

Proyecto de práctica de DevOps (ITSON). Es una pequeña aplicación Java que calcula **cobros de estacionamiento**, construida para que practiques, con un caso realista y **con problemas escondidos a propósito**, todo el camino de un cambio hasta un artefacto ejecutable:

Git y ramas → Maven → GitHub Actions → pruebas JUnit → JaCoCo → SonarQube Cloud → Nexus → Docker → AWS EC2.

## Qué hace la aplicación
- **Tarifa**: ≤ 15 min gratis; 16–60 min $10; después $10 + $8 por hora o fracción; tope $80 por visita; minutos negativos son inválidos.
- **PaseDescuento**: códigos `ITSON20` (20 %) y `EGRESADO10` (10 %), sin distinguir mayúsculas ni espacios; vacío o nulo = 0 %; otro código es inválido.
- **CobroService**: aplica el descuento a la tarifa.
- **Estacionamiento**: cupo limitado; ingresar, salir, ocupados y disponibles.
- **Reporte**: porcentaje de ocupación y etiqueta BAJA / MEDIA / ALTA (cortes en 50 y 90).
- **ParqueoServer**: servidor HTTP con `/health` y `/api/cobro?minutos=..&pase=..`.
- **avisos**: segundo módulo, genera el texto de un recibo.

> El código **tiene errores plantados** (de cálculo, de límites, de calidad y de seguridad). Encontrarlos con pruebas y con SonarQube es parte de la práctica. No están listados aquí a propósito.

## Estructura
```
parqueo-track/
├── pom.xml                  proyecto padre (multimódulo)
├── parqueo-api/             módulo principal (lógica + servidor HTTP)
├── avisos/                  segundo módulo
├── .github/workflows/ci.yml workflow de CI inicial
└── docs/
    ├── guia/                FASE-00 … FASE-11 (sigue en orden)
    └── plantillas/          calidad.yml, Dockerfile, settings.xml, ADR, etc.
```

## Requisitos
- Java 21 (JDK) y Maven 3.9+ (`java -version`, `mvn -version`)
- Git y una cuenta de GitHub
- Docker Desktop (para las Fases 8 y 9)
- Cuenta de SonarQube Cloud (Fase 7)
- Cuenta de AWS (Fase 10; opcional pero recomendada)
- Una terminal (PowerShell en Windows). Un editor como VS Code.

## Ruta de práctica
| Fase | Tema | Relación con el curso |
|---|---|---|
| 0 | Preparación, Git, primer push, CI en rojo | Sem. 1 |
| 1 | Build sin IDE con Maven | Sem. 2 |
| 2 | CI con GitHub Actions | Sem. 2–3 |
| 3 | Ramas, PR, conflictos, ADR | Sem. 4 |
| 4 | Pruebas AAA y `assertThrows` | Unidad 1 |
| 5 | Parametrizadas, TDD, Avance 2 | Unidad 1 |
| 6 | Git avanzado (stash, amend, add -p, revert) | Todo el curso |
| 7 | JaCoCo, SonarQube Cloud, protección de rama | Avance 1 |
| 8 | Nexus y `mvn deploy` | Sem. 6 · Avance 3 |
| 9 | Docker y registro en Nexus | Sem. 6 · Avance 3 |
| 10 | AWS EC2 | Servidor del equipo |
| 11 | Cierre y demo en vivo | Avance 3 |

## Reglas de trabajo
1. Nada de commits directos a `main`: rama → PR → CI verde → merge.
2. **Nunca** subas `settings.xml`, tokens ni llaves `.pem`.
3. Nunca modifiques un `assert` para que una prueba pase: si la prueba falla, el defecto está en el código (o en tu comprensión de la regla).
4. Cada fase termina con evidencia (captura, link, salida de comando).

## Antes de empezar
Cambia `TU_USUARIO` en `pom.xml` (propiedades `sonar.*`) cuando llegues a la Fase 7; no lo necesitas antes. Empieza por `docs/guia/FASE-00-preparacion.md`.

## Problemas frecuentes
- `mvn` no se reconoce → revisa `PATH` y abre una terminal nueva.
- Docker no arranca → `wsl --shutdown`, cierra Docker Desktop y ábrelo de nuevo.
- Nexus tarda en estar listo → 2–3 min; revisa `docker logs -f nexus-lab`.
