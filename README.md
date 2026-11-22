<div align="center">

# 🎓 Learning Platform

**School management and adaptive math practice: one system for admins, managers, teachers and students.**

Spring Boot API · React client · JWT + SMS 2FA · Live notifications (SSE) · AI tutor · 31 auto-generated question types

[**Live app**](https://schoolmanagmentsystem-z0m5.onrender.com) · [**API**](https://learning-project-server.onrender.com/swagger-ui.html)

</div>

---

## Repository layout

| Folder | What | Stack | Readme |
|---|---|---|---|
| [`backend/`](backend) | REST API, SSE notifications, question engine, scheduled jobs | Java 17, Spring Boot 3.4, JPA, MySQL, Ehcache | [backend/README.md](backend/README.md) |
| [`frontend/`](frontend) | Web app with a dashboard per role | React 19, Vite 6, Tailwind 4, daisyUI | [frontend/README.md](frontend/README.md) |
| [`docs/`](docs) | System spec and caching map | | [SPEC](docs/SPEC.md) · [CACHING](docs/CACHING.md) |

This repository joins the former `Learning-Project-Server` and `SchoolManagmentSystem` repositories, with the full history of both.

## Run locally

Prerequisites: Java 17, MySQL 8 (database `learning`), Node 20+.

```bash
# backend → http://localhost:8080
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# frontend → http://localhost:5173
cd frontend
npm install
npm run dev
```

To use the local API, set `API_BASE_URL` in `frontend/src/constants/base.constants.js` to `http://localhost:8080` (don't commit it). Backend secrets (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GPT_TOKEN`, `SMS_TOKEN`) go in `backend/.env`, which git ignores.

## Working with Claude Code

`CLAUDE.md` (root, `backend/`, `frontend/`) holds the working rules for each part. The codebase has a graphify knowledge graph in `graphify-out/` (local, git-ignored, rebuilt by git hooks after every commit, merge and checkout).

## 👤 Author

**Shai Berkovich**, B.Sc. Computer Science · [GitHub](https://github.com/shaiberkov)
