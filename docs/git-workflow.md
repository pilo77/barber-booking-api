# Git Workflow

## Ramas

Ramas protegidas:

- `main`
- `master`
- `develop`
- `qa`

No trabajar directamente sobre ramas protegidas.

Ramas de trabajo:

```text
feature/HU-01-project-foundation
feature/HU-02-customer-crud
feature/HU-03-barber-crud
feature/HU-04-service-crud
feature/HU-05-barber-schedule
feature/HU-06-book-appointment
feature/HU-07-barber-availability
```

## Commits

Usar Conventional Commits en ingles:

```text
chore(project): bootstrap barber booking api foundation
feat(customers): add customer creation use case
test(appointments): prevent overlapping appointments
fix(availability): ignore cancelled appointments
docs(api): document booking endpoints
```

## Antes de commitear

Ejecutar:

```powershell
git status --short
git diff
.\mvnw.cmd clean verify
```

No commitear `.env`, logs, dumps, credenciales, tokens, builds ni archivos
temporales.
