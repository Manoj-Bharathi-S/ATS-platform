# Use an official lightweight Python image
FROM python:3.10-slim

# Set working directory inside the container
WORKDIR /app

# Copy dependency list and install them
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code
COPY . .

# Render dynamically passes a port, use $PORT with a fallback to 8000
EXPOSE 8000

# Start Uvicorn, binding to 0.0.0.0 and Render's dynamic port
CMD sh -c "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}"