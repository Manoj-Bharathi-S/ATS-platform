from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from motor.motor_asyncio import AsyncIOMotorClient
import pdfplumber
import io
import uuid
import re
from fastapi.middleware.cors import CORSMiddleware # <-- CRITICAL IMPORT

app = FastAPI(title="Resume Processing Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins for local testing
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods (GET, POST, OPTIONS, etc.)
    allow_headers=["*"],  # Allows all headers (Content-Type, Authorization, etc.)
)
# MongoDB connection string from your docker-compose.yml
# Use 'mongodb' if running inside docker, otherwise 'localhost'
MONGO_URI = "mongodb://root_admin:root_password@10.198.74.72:27017/resume_processing_db?authSource=admin"

# Create a global database client variable
db_client = None
db = None

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

# --- Helper Functions for Parsing ---

def extract_text_from_pdf(file_bytes: bytes) -> str:
    """Reads PDF bytes and extracts text using pdfplumber."""
    text = ""
    try:
        # Wrap bytes in a file-like object for pdfplumber
        with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
        return text
    except Exception as e:
        raise ValueError(f"Error reading PDF: {str(e)}")

def extract_skills(text: str) -> list[str]:
    """A basic keyword extraction function."""
    # In a real app, you would use spaCy here for NLP.
    # For now, we will look for a predefined list of skills.
    known_skills = [
        "python", "java", "spring boot", "react", "mysql", 
        "mongodb", "docker", "kubernetes", "aws", "git"
    ]
    
    found_skills = set()
    text_lower = text.lower()
    
    for skill in known_skills:
        # Use regex to find whole words only
        if re.search(r'\b' + re.escape(skill) + r'\b', text_lower):
            found_skills.add(skill.title()) # Capitalize nicely
            
    return list(found_skills)


# --- API Endpoints ---

@app.post("/api/v1/resumes/upload")
async def upload_resume(file: UploadFile = File(...)):
    if not file.filename.endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Only PDF files are supported.")
    
    # 1. Read file bytes
    try:
        contents = await file.read()
    except Exception:
        raise HTTPException(status_code=500, detail="Could not read the uploaded file.")
    
    # 2. Extract Text
    try:
        raw_text = extract_text_from_pdf(contents)
    except ValueError as e:
         raise HTTPException(status_code=400, detail=str(e))
         
    if not raw_text.strip():
        raise HTTPException(status_code=400, detail="PDF text could not be extracted or is empty.")

    # 3. Process Text (Extract Skills)
    skills = extract_skills(raw_text)
    
    # 4. Build Document Structure
    candidate_id = str(uuid.uuid4())
    resume_document = {
        "candidate_id": candidate_id,
        "filename": file.filename,
        "extracted_skills": skills,
        # We store the raw text in case we want to re-parse it later with better algorithms
        "raw_text": raw_text[:500] + "..." if len(raw_text) > 500 else raw_text 
    }
    
    # 5. Save to MongoDB
    try:
        await db.candidates.insert_one(resume_document)
    except Exception as e:
         raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")

    # 6. Return Success Response
    # The _id added by Mongo is an ObjectId, which isn't JSON serializable by default,
    # so we delete it from the dict before returning, relying on our candidate_id.
    if "_id" in resume_document:
        del resume_document["_id"]
        
    return JSONResponse(status_code=201, content={
        "message": "Resume processed successfully.",
        "data": resume_document
    })

@app.get("/")
def home():
    return {"status":"okay"}

@app.get("/api/v1/resumes/{candidate_id}")
async def get_resume(candidate_id: str):
    candidate = await db.candidates.find_one({"candidate_id": candidate_id})
    if not candidate:
         raise HTTPException(status_code=404, detail="Candidate not found")
         
    if "_id" in candidate:
        del candidate["_id"]
        
    return candidate