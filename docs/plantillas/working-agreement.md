# Working Agreement — ParqueoTrack

> Un working agreement es un acuerdo explícito de cómo trabajamos. Pocas reglas claras, que se puedan verificar.
> (Semana 1 del curso.) Si trabajas solo, escribe los acuerdos que te impones a ti mismo.

## Cambios
- Commits pequeños y con mensaje claro: `tipo: qué cambió` (tipos: `feat`, `fix`, `test`, `ci`, `build`, `docs`, `refactor`, `chore`).
- Nada de trabajo directo en `main`: cada cambio entra por una rama y un Pull Request.

## Calidad
- Antes de subir un cambio, el proyecto compila y las pruebas pasan en mi máquina (`mvn -B verify`).
- Un PR no se mergea con el CI en rojo.
- Nunca se corrige un rojo cambiando el `assert` para que "pase": se corrige el código (o se discute la regla).

## Seguridad
- Cero credenciales, tokens, llaves `.pem` ni `settings.xml` en el repositorio.
- Los secretos viven en GitHub Secrets o en archivos locales fuera del repo.

## Comunicación
- Si estoy bloqueado más de 30 minutos, lo digo (con el error completo, no "no funciona").

## Colaboración
- Nadie es el único que sabe cómo se publica o se despliega: todos pueden repetir el flujo.
