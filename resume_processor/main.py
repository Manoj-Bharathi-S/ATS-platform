from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from motor.motor_asyncio import AsyncIOMotorClient
import pdfplumber
import io
import uuid
import re
from fastapi.middleware.cors import CORSMiddleware
import os

app = FastAPI(title="Resume Processing Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MONGO_URI = os.environ.get("MONGO_URI", "mongodb://localhost:27017")

db_client = None
db = None

# Canonical display name -> search aliases. Matching keys strip punctuation/spaces.
SKILL_CATALOG: dict[str, list[str]] = {
    "Python": ["python", "python3", "python 3"],
    "Java": ["java"],
    "JavaScript": ["javascript", "java script", "js", "ecmascript"],
    "TypeScript": ["typescript", "ts"],
    "C": ["c language"],
    "C++": ["c++", "cpp", "c plus plus", "cplusplus"],
    "C#": ["c#", "csharp", "c sharp"],
    "Go": ["golang", "go lang"],
    "Rust": ["rust"],
    "Ruby": ["ruby", "ruby on rails", "rails"],
    "PHP": ["php"],
    "Swift": ["swift"],
    "Kotlin": ["kotlin"],
    "Scala": ["scala"],
    "R": ["r language"],
    "SQL": ["sql"],
    "HTML": ["html", "html5"],
    "CSS": ["css", "css3"],
    "React": ["react", "react.js", "reactjs", "react js"],
    "Angular": ["angular", "angularjs", "angular.js"],
    "Vue": ["vue", "vue.js", "vuejs"],
    "Next.js": ["next.js", "nextjs", "next js"],
    "Node.js": ["node.js", "nodejs", "node js", "node"],
    "Express": ["express", "express.js", "expressjs"],
    "Spring": ["spring", "spring framework", "springframework"],
    "Spring Boot": ["spring boot", "springboot", "spring-boot"],
    "Hibernate": ["hibernate"],
    "Django": ["django"],
    "Flask": ["flask"],
    "FastAPI": ["fastapi", "fast api"],
    "REST": ["rest", "rest api", "restful"],
    "GraphQL": ["graphql"],
    "gRPC": ["grpc"],
    "MySQL": ["mysql"],
    "PostgreSQL": ["postgresql", "postgres", "psql"],
    "MongoDB": ["mongodb", "mongo"],
    "Redis": ["redis"],
    "Oracle": ["oracle", "oracle db", "oracle database"],
    "SQL Server": ["sql server", "mssql", "ms sql"],
    "Elasticsearch": ["elasticsearch", "elastic search"],
    "Kafka": ["kafka", "apache kafka"],
    "RabbitMQ": ["rabbitmq", "rabbit mq"],
    "Docker": ["docker"],
    "Kubernetes": ["kubernetes", "k8s"],
    "AWS": ["aws", "amazon web services", "amazon aws"],
    "Azure": ["azure", "microsoft azure"],
    "GCP": ["gcp", "google cloud", "google cloud platform"],
    "Git": ["git"],
    "GitHub": ["github"],
    "GitLab": ["gitlab"],
    "CI/CD": ["ci/cd", "cicd", "ci cd", "continuous integration"],
    "Jenkins": ["jenkins"],
    "Terraform": ["terraform"],
    "Linux": ["linux"],
    "Unix": ["unix"],
    "Pandas": ["pandas"],
    "NumPy": ["numpy"],
    "TensorFlow": ["tensorflow", "tensor flow"],
    "PyTorch": ["pytorch", "py torch"],
    "Scikit-learn": ["scikit-learn", "sklearn", "scikit learn"],
    "Spark": ["spark", "apache spark", "pyspark"],
    "Hadoop": ["hadoop"],
    "Airflow": ["airflow", "apache airflow"],
    "Tableau": ["tableau"],
    "Power BI": ["power bi", "powerbi"],
    "Excel": ["excel", "microsoft excel", "ms excel"],
    "Jira": ["jira"],
    "Agile": ["agile", "scrum"],
    "Microservices": ["microservices", "micro services", "microservice"],
    "OOP": ["oop", "object oriented", "object-oriented"],
    "Data Structures": ["data structures", "dsa"],
    "Machine Learning": ["machine learning", "ml"],
    "Deep Learning": ["deep learning"],
    "NLP": ["nlp", "natural language processing"],
    "LLM": ["llm", "large language model", "large language models"],
    "OpenAI": ["openai", "gpt", "chatgpt"],
    "LangChain": ["langchain", "lang chain"],
    "Selenium": ["selenium"],
    "JUnit": ["junit"],
    "Maven": ["maven"],
    "Gradle": ["gradle"],
    "Webpack": ["webpack"],
    "Redux": ["redux"],
    "SASS": ["sass", "scss"],
    "Tailwind": ["tailwind", "tailwindcss", "tailwind css"],
    "Bootstrap": ["bootstrap"],
    "Figma": ["figma"],
    "Android": ["android"],
    "iOS": ["ios"],
    "React Native": ["react native", "reactnative"],
    "Flutter": ["flutter"],
    ".NET": [".net", "dotnet", "dot net", "asp.net", "aspnet"],
}

SECTION_STOPWORDS = {
    "and", "or", "the", "with", "using", "including", "proficient", "experienced",
    "knowledge", "strong", "skills", "technical", "tools", "technologies", "frameworks",
    "languages", "libraries", "platforms", "etc", "in", "of", "to", "for", "a", "an",
    "on", "as", "via", "from", "years", "year", "plus", "good", "working", "familiar",
    "expert", "advanced", "intermediate", "basic", "core", "competencies", "stack",
}

SKILLS_SECTION_RE = re.compile(
    r"(?is)(?:^|\n)\s*(?:technical\s+)?(?:skills?|skill\s+set|tech(?:nical)?\s+stack|"
    r"technologies|core\s+competencies|tools(?:\s*(?:&|and)\s*technologies)?)\s*[:\-]?\s*"
    r"(.*?)(?=\n\s*(?:experience|education|projects?|certifications?|summary|objective|"
    r"work\s+history|employment|awards|languages|interests|references|publications|"
    r"achievements|volunteer)\b|\Z)"
)


def matching_key(skill: str) -> str:
    """Normalize a skill into a punctuation-free key used for matching."""
    if not skill:
        return ""
    s = skill.lower().strip()
    s = s.replace("c++", "cpp").replace("c#", "csharp").replace("f#", "fsharp")
    s = s.replace(".net", "dotnet")
    return re.sub(r"[^a-z0-9]", "", s)


ALIAS_TO_CANONICAL: dict[str, str] = {}
CANONICAL_BY_KEY: dict[str, str] = {}
for _canonical, _aliases in SKILL_CATALOG.items():
    CANONICAL_BY_KEY[matching_key(_canonical)] = _canonical
    ALIAS_TO_CANONICAL[matching_key(_canonical)] = _canonical
    for _alias in _aliases:
        ALIAS_TO_CANONICAL[matching_key(_alias)] = _canonical


def canonical_name(skill: str) -> str | None:
    key = matching_key(skill)
    if not key:
        return None
    return ALIAS_TO_CANONICAL.get(key) or CANONICAL_BY_KEY.get(key)


@app.on_event("startup")
async def startup_db_client():
    global db_client, db
    db_client = AsyncIOMotorClient(MONGO_URI)
    db = db_client.resume_processing_db
    print("Connected to MongoDB!")


@app.on_event("shutdown")
async def shutdown_db_client():
    if db_client:
        db_client.close()


def extract_text_from_pdf(file_bytes: bytes) -> str:
    """Reads PDF bytes and extracts text using pdfplumber, including table cells."""
    parts: list[str] = []
    try:
        with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text(x_tolerance=2, y_tolerance=3)
                if page_text:
                    parts.append(page_text)
                for table in page.extract_tables() or []:
                    for row in table:
                        cells = [cell.strip() for cell in row if cell and str(cell).strip()]
                        if cells:
                            parts.append(" | ".join(cells))
        return "\n".join(parts)
    except Exception as e:
        raise ValueError(f"Error reading PDF: {str(e)}")


def _normalize_resume_text(text: str) -> str:
    text = text.replace("\u00a0", " ")
    text = re.sub(r"[•●▪◦‣∙]", ",", text)
    text = re.sub(r"[ \t]+", " ", text)
    return text


def _alias_search_pattern(alias: str) -> re.Pattern:
    tokens = [re.escape(tok) for tok in re.split(r"[\s._/\-]+", alias.lower()) if tok]
    if not tokens:
        tokens = [re.escape(alias.lower())]
    joined = r"[\s._/\-]*".join(tokens)
    return re.compile(rf"(?<![A-Za-z0-9]){joined}(?![A-Za-z0-9])", re.IGNORECASE)


def _looks_like_skill(token: str) -> bool:
    cleaned = token.strip(" .:-/|")
    if not cleaned:
        return False
    lower = cleaned.lower()
    if lower in SECTION_STOPWORDS:
        return False
    if len(cleaned) > 40 or len(cleaned) < 1:
        return False
    if not re.search(r"[A-Za-z+#]", cleaned):
        return False
    if re.fullmatch(r"\d{4}", cleaned):
        return False
    if " " in cleaned and len(cleaned.split()) > 4:
        return False
    if lower in {"http", "https", "www", "com"}:
        return False
    return True


def _tokens_from_skills_section(text: str) -> list[str]:
    tokens: list[str] = []
    for match in SKILLS_SECTION_RE.finditer(text):
        blob = match.group(1)
        for raw in re.split(r"[,;|/\\\n]| {2,}", blob):
            piece = re.sub(r"\s+", " ", raw).strip(" \t.:-")
            if _looks_like_skill(piece):
                tokens.append(piece)
    return tokens


def extract_skills(text: str) -> list[str]:
    """Extract skills from resume text via catalog matching and skills-section parsing."""
    if not text:
        return []

    normalized = _normalize_resume_text(text)
    compact = re.sub(r"\s+", " ", normalized)
    found: dict[str, str] = {}

    # Short tokens like "js" also appear inside "React.js" / "Node.js".
    full_text_skip = {"c", "r", "go", "js", "ts", "ml", "node"}
    for canonical, aliases in SKILL_CATALOG.items():
        for alias in aliases:
            if matching_key(alias) in full_text_skip:
                continue
            pattern = _alias_search_pattern(alias)
            if pattern.search(compact) or pattern.search(normalized):
                found[matching_key(canonical)] = canonical
                break

    for token in _tokens_from_skills_section(normalized):
        mapped = canonical_name(token)
        if mapped:
            found[matching_key(mapped)] = mapped
        elif _looks_like_skill(token):
            key = matching_key(token)
            if key and key not in found:
                found[key] = token.strip()

    return sorted(found.values(), key=str.lower)


@app.post("/api/v1/resumes/upload")
async def upload_resume(file: UploadFile = File(...)):
    if not file.filename.endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Only PDF files are supported.")

    try:
        contents = await file.read()
    except Exception:
        raise HTTPException(status_code=500, detail="Could not read the uploaded file.")

    try:
        raw_text = extract_text_from_pdf(contents)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    if not raw_text.strip():
        raise HTTPException(status_code=400, detail="PDF text could not be extracted or is empty.")

    skills = extract_skills(raw_text)

    candidate_id = str(uuid.uuid4())
    resume_document = {
        "candidate_id": candidate_id,
        "filename": file.filename,
        "extracted_skills": skills,
        "raw_text": raw_text[:500] + "..." if len(raw_text) > 500 else raw_text
    }

    try:
        await db.candidates.insert_one(resume_document)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

    if "_id" in resume_document:
        del resume_document["_id"]

    return JSONResponse(status_code=201, content={
        "message": "Resume processed successfully.",
        "data": resume_document
    })


@app.get("/")
def home():
    return {"status": "okay"}


@app.get("/api/v1/resumes/{candidate_id}")
async def get_resume(candidate_id: str):
    candidate = await db.candidates.find_one({"candidate_id": candidate_id})
    if not candidate:
        raise HTTPException(status_code=404, detail="Candidate not found")

    if "_id" in candidate:
        del candidate["_id"]

    return candidate
