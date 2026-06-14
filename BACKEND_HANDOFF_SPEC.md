# DueProcess AI - Backend Handoff Spec

## Overview
This document specifies the exact API contracts and architectural patterns the backend needs to implement to connect with the DueProcess Android App. The Android app is built offline-first using Room Database and Jetpack Compose.

## 1. Authentication (JWT/OAuth)
The Android app expects standard stateless session handling.
* **POST `/api/v1/auth/login`**
  * **Requires:** `email`, `password` or biometric key.
  * **Returns:** `{ "accessToken": "jwt...", "refreshToken": "jwt...", "userId": "..." }`

## 2. The Offline-First Sync Pattern
The Android app **never reads directly from the API for UI**. It uses an "Offline-First" Repository pattern:
1. UI subscribes to the Local SQLite database (Room).
2. App fetches from Backend.
3. App writes to Local SQLite database.
4. UI auto-updates.

**Backend Requirement:** Endpoints must support pagination and potentially `"updated_since"` timestamp parameters so the mobile app can request delta-syncs rather than downloading the entire case database every time.

* **GET `/api/v1/cases?updated_since=1690000000`**
  * **Returns:** A list of `Case` objects modified since that timestamp.

## 3. Large Evidence Uploads (Presigned URLs - CRITICAL)
Legal evidence (PDFs, Transcripts, Audio) is huge. The Android app **MUST NOT** send multipart form uploads directly to your backend API servers, or it will crash your memory.

**The Workflow:**
1. **Android calls:** `POST /api/v1/documents/upload-url`
   * *Payload:* `{ "filename": "deposition.pdf", "mimeType": "application/pdf", "caseId": "..." }`
2. **Backend returns:** A secure signed URL (AWS S3 Presigned URL, Google Cloud Storage signed URL, or Azure SAS) and a `documentId`.
3. **Android uploads:** Puts the binary file directly into the cloud bucket using that URL.
4. **Android calls:** `POST /api/v1/documents/confirm`
   * *Payload:* `{ "documentId": "..." }` 
   * *Action:* Backend triggers OCR / Agentic extraction pipeline.

## 4. Swarm / Agent Execution (Async Polling or WebSockets)
When a user clicks "Initiate Swarm Protocol":
* **POST `/api/v1/agents/run`**
  * *Payload:* `{ "caseId": "...", "scope": ["DOC-1", "DOC-2"], "agentTypes": ["canon_hunter", "contradiction_engine"] }`
  * *Returns:* `{ "runId": "RUN-992", "status": "processing" }`

Because agents take minutes to run, the backend must expose a polling endpoint (or WebSocket):
* **GET `/api/v1/agents/run/{runId}/status`**
  * *Returns:* Progress logs, percent complete, and any real-time findings.

## 5. Findings Engine (Provenance Tracking)
Every finding the backend returns MUST have "Source Anchors". This is non-negotiable for a legal app.
* **GET `/api/v1/cases/{caseId}/findings`**
  * *Returns elements like:*
```json
{
  "findingId": "FND-104",
  "severity": "CRITICAL",
  "description": "Contradiction in timeline of events.",
  "confidenceScore": 96,
  "sourceAnchors": [
    {
      "documentId": "DOC-042",
      "textQuote": "I arrived at 9:00 PM.",
      "page": 4
    },
    {
      "documentId": "DOC-088",
      "textQuote": "Camera footage shows entry at 11:14 PM.",
      "page": 1
    }
  ]
}
```
