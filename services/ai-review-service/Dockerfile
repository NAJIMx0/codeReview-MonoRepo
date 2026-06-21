FROM python:3.13-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends gcc librdkafka-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

# -u forces Python to flush stdout immediately instead of buffering it,
# which is essential in Docker — without this, print() output can sit
# in a buffer and never reach `docker compose logs`.
CMD ["python", "-u", "main.py"]