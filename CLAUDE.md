# CLAUDE.md

## Propósito

Este archivo guía a Claude Code dentro del proyecto `barber-booking-api`.

Claude debe leer también `AGENTS.md`. Si hay conflicto entre archivos, respetar la regla más restrictiva.

## Reglas operativas para Claude Code

- Responder en español.
- No modificar archivos sin entender la tarea.
- No hacer push, PR, deploy, merge ni tocar `main` sin autorización explícita.
- No ejecutar comandos destructivos sin confirmación explícita.
- No commitear `.codex/`, `.env`, logs locales ni archivos temporales.
- No modificar migraciones Flyway existentes.
- No mezclar cambios funcionales con chores de documentación o configuración.

## Flujo recomendado

Antes de cualquier tarea:

```powershell
pwd
git branch --show-current
git status --short
git log --oneline --decorate -n 10
```

Para validar:

```powershell
.\mvnw.cmd verify
git diff --check
git status --short
```

## Modo de trabajo

* Para implementación: cambiar solo lo necesario.
* Para auditoría: revisar código, migraciones, tests y seguridad antes de proponer merge.
* Para bugs pequeños: corregir, testear y crear commit convencional.
* Para riesgos grandes: no improvisar; reportar hallazgo y pedir decisión.

## Reporte final mínimo

Todo cierre debe incluir:

* Rama actual.
* Archivos modificados.
* Commit creado si aplica.
* Tests ejecutados.
* Resultado de `mvnw verify`.
* Resultado de `git diff --check`.
* Resultado de `git status --short`.
* Riesgos/deuda.
* Recomendación final.
