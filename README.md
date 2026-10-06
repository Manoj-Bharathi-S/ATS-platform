# ATS — Applicant Tracking System

A small microservice ATS for creating jobs, uploading PDF resumes, extracting skills, and scoring how well a candidate matches a job.

```mermaid
flowchart LR
  UI[React dashboard :5173]
  Jobs[Job service :8080]
  Resumes[Resume processor :8000]
  Score[Scoring engine :8081]
  MySQL[(MySQL)]
  Mongo[(MongoDB)]

  UI --> Jobs
  UI --> Resumes
  UI --> Score
  Jobs --> MySQL
  Resumes --> Mongo
  Score --> Jobs
  Score --> Resumes
```

## What’s in the repo

| Path | Service | Stack | Default port |
| --- | --- | --- | --- |
| `frontend/` | Dashboard (create jobs, upload PDFs, run scores) | React + Vite | `5173` |
| `site/` | Job management API | Spring Boot 4, Java 21, JPA | `8080` |
| `resume_processor/` | PDF parse + skill extraction | FastAPI, pdfplumber, Motor | `8000` |
| `scoring/` | Match score between job and candidate | Spring Boot 4, OpenFeign | `8081` |

`docker-compose.yaml` starts **MySQL 8** and **MongoDB 6** only. App services are run locally (or from their Dockerfiles).

## Prerequisites

- Java 21
- Python 3.10+
- Node.js 18+
- Docker Desktop (for the databases)

## Run locally

### 1. Databases

```bash
docker compose up -d
```

This starts:

- MySQL at `localhost:3306` — database `job_management_db`, user `ats_user` / `ats_password`
- MongoDB at `localhost:27017` — root user `root_admin` / `root_password`

These credentials are for local development only.

### 2. Job service

```bash
cd site
./mvnw spring-boot:run
```

On Windows: `.\mvnw.cmd spring-boot:run`.

Listens on [http://localhost:8080](http://localhost:8080). Hibernate creates/updates tables on startup (`ddl-auto=update`).

### 3. Resume processor

```bash
cd resume_processor
pip install -r requirements.txt
```

Mongo in Compose is started with authentication, so set a URI that includes the root user:

```bash
# PowerShell
$env:MONGO_URI = "mongodb://root_admin:root_password@localhost:27017/resume_processing_db?authSource=admin"

# bash
export MONGO_URI="mongodb://root_admin:root_password@localhost:27017/resume_processing_db?authSource=admin"

uvicorn main:app --reload --port 8000
```

API docs: [http://localhost:8000/docs](http://localhost:8000/docs).

### 4. Scoring engine

```bash
cd scoring
./mvnw spring-boot:run
```

Listens on [http://localhost:8081](http://localhost:8081). It calls the job and resume services over HTTP (OpenFeign).

Optional overrides:

```text
JOB_SERVICE_URL=http://localhost:8080
RESUME_SERVICE_URL=http://localhost:8000
```

### 5. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

Optional env (Vite):

```text
VITE_JOB_SERVICE_URL=http://localhost:8080
VITE_RESUME_SERVICE_URL=http://localhost:8000
VITE_SCORING_SERVICE_URL=http://localhost:8081
```

## Typical flow

1. **Jobs** — create a posting with comma-separated required skills (for example `Java, Spring Boot, MySQL`).
2. **Resumes** — upload a PDF. The processor extracts text (including table cells), finds skills from a catalog plus a Skills section, and stores a `candidate_id`.
3. **Scoring** — pick a job and paste the candidate id. The engine compares required skills to extracted skills using aliases (`React.js` ≈ `React`, `k8s` ≈ `Kubernetes`) and returns a match percentage plus matched/missing skills.

Re-upload a resume after changing extraction logic; existing Mongo documents are not re-parsed.

## HTTP APIs

**Job service** (`site`)

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/jobs` | Create a job (`title`, `description`, `requiredSkills`, `minExperienceYears`) |
| `GET` | `/jobs` | List jobs |
| `GET` | `/jobs/{id}` | Get one job |

**Resume processor**

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/resumes/upload` | Multipart PDF upload; returns `candidate_id` and `extracted_skills` |
| `GET` | `/api/v1/resumes/{candidate_id}` | Fetch stored candidate |

**Scoring**

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/score?jobId={id}&candidateId={uuid}` | Compute match score |

## Environment variables

| Variable | Used by | Default |
| --- | --- | --- |
| `PORT` | Job / scoring / resume (Docker) | `8080` / `8081` / `8000` |
| `DB_URL` | Job service | `jdbc:mysql://localhost:3306/job_management_db` |
| `DB_USER` / `DB_PASSWORD` | Job service | `ats_user` / `ats_password` |
| `MONGO_URI` | Resume processor | `mongodb://localhost:27017` (use the auth URI above with Compose) |
| `JOB_SERVICE_URL` | Scoring | `http://localhost:8080` |
| `RESUME_SERVICE_URL` | Scoring | `http://localhost:8000` |

## Docker images (optional)

Each backend has a `Dockerfile` (Java 21 multi-stage Maven builds; Python `uvicorn` on `$PORT`). Compose does not start those images today—wire them onto `ats-network` if you want a fully containerized stack.

## Tests

```bash
cd scoring
./mvnw -Dtest=SkillMatcherTest test
```

Skill matching is covered in `scoring/src/test/java/com/ats/scoring/service/SkillMatcherTest.java`.
