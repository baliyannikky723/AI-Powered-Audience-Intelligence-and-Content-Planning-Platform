# Multi-Format Content Exporter & Production Packager (Phase 3L)

## 1. Executive Summary & Architecture

The Multi-Format Content Exporter and Production Packager translates creator-approved AI production drafts into production-ready deliverable packages across multiple publishing-agnostic formats.

### Core Workflow
```
AUDIENCE EVIDENCE
      ↓
RECOMMENDATION
      ↓
CALENDAR ITEM
      ↓
PRODUCTION ASSET
      ↓
CREATOR APPROVAL (APPROVED)
      ↓
EXPORT / PRODUCTION PACKAGE (Phase 3L)
      ↓
DOWNLOAD / CREATOR HANDOFF
```

```
+---------------------------------------------------------------------------------------------------+
|                                  DETERMINISTIC PACKAGING ENGINE                                   |
|                                                                                                   |
|  [Approved Asset] ----> [ExportFormattingService] ----> [SHA-256 Digest] ----> [Audit Logging]     |
|   - Content Brief             - Markdown Converter          - Hex Format           - EXPORT_CREATED  |
|   - Script Outline            - PDF Styler (OpenPDF)        - Integrity Seal       - PACKAGE_CREATED |
|   - Teleprompter TXT          - Teleprompter Pacer                                                   |
|   - Scene Timeline            - Zip Stream Packager                                                  |
|   - Evidence IDs                                                                                  |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Supported Export Formats

| Export Type | File Extension | MIME Type | Target Use-Case |
|---|---|---|---|
| `MARKDOWN` | `.md` | `text/markdown` | Clean markdown brief, outline, talking points, and sanitized evidence citations. |
| `PDF` | `.pdf` | `application/pdf` | Professional print/studio PDF package with color palette, cover, tables, and provenance appendix. |
| `TELEPROMPTER` | `.txt` | `text/plain` | High-contrast, all-caps cue markers (`[HOOK]`, `[INTRO]`, `[POINT 1]`, `[CTA]`) optimized for prompting software. |
| `CHECKLIST` | `.md` | `text/markdown` | Chronological Pre-Production, Production, and Post-Production quality assurance checklist. |
| `JSON` | `.json` | `application/json` | Machine-readable canonical schema including content brief, titles, hooks, outline, timeline, and provenance. |
| `TIMELINE` | `.md` | `text/markdown` | Structured scene breakdown table with purpose, talking points, and estimated duration. |
| `PRODUCTION_PACKAGE` | `.zip` | `application/zip` | Complete multi-file archive containing all 13 individual documents, PDF pack, content JSON, and provenance README. |

---

## 3. Production Package ZIP Specification

The complete archive bundle (`.zip`) packages the following 13 artifacts generated deterministically from the approved asset state:

```
pulsegpt-production-pack-{assetId}-v{version}.zip
├── content-brief.md         # Full creative strategy and content brief
├── script-outline.md        # Comprehensive structured outline
├── teleprompter.txt         # Studio teleprompter formatted text
├── titles.md                # Title variations with rationale
├── hooks.md                 # Opening hook variations with rationale
├── ctas.md                  # Call-to-action variants
├── thumbnail-prompt.md      # Thumbnail visual concepts and text overlays
├── checklist.md             # Pre, pro, and post production checklists
├── timeline.md              # Scene breakdown duration estimates
├── evidence.json            # Sanitized audience evidence signals
├── content.json             # Canonical JSON production export
├── production-pack.pdf      # Complete formatted OpenPDF document
└── README.md                # Provenance, integrity hash, and handoff notice
```

---

## 4. Deterministic Export Rule (Zero LLM Invocations)

> **CRITICAL RULE**: Exporting does **NOT** invoke an LLM.
- **LLM Call Count During Export = 0**
- Output is generated strictly from the validated, creator-approved database snapshot.
- Guarantees 100% reproducibility, zero token consumption on export, and prevents unapproved hallucinated edits.

---

## 5. Security, Tenancy & Eligibility

1. **Eligibility Enforcement**: Only assets with status `APPROVED` are exportable. Any attempt to export `GENERATED`, `VALIDATED`, `REJECTED`, or `ARCHIVED` assets immediately returns `HTTP 400 Bad Request` with error code `ASSET_NOT_APPROVED`.
2. **Cross-Tenant Isolation**: Export generation, retrieval, and binary downloads are strictly user-scoped via JWT authentication. Cross-user access returns `HTTP 404 Not Found` (`ASSET_NOT_FOUND` / `EXPORT_NOT_FOUND`).
3. **Safe Filenames**: Sanitized filenames prevent directory traversal (`pulsegpt-production-{assetId}-v{version}.{ext}`).
4. **PII Sanitization**: Audience evidence strings are scrubbed of raw PII before document embedding.

---

## 6. SHA-256 Integrity Hashing

Every exported file and ZIP bundle is hashed using SHA-256 upon generation.
- The 64-character lowercase hex digest is persisted in `content_exports.content_hash`.
- Download responses supply this integrity seal via the `X-Content-Hash` HTTP response header.
- Creators and third-party systems can verify byte-for-byte tamper resistance.

---

## 7. Research Instrumentation Metrics

Each export operation logs and persists operational telemetry:
- `exportType`: Target deliverable format.
- `version`: Production asset version.
- `generationMode`: `EVIDENCE_GROUNDED` vs `BASELINE`.
- `exportLatencyMs`: Time taken to build and format artifact (typically < 35ms).
- `fileSizeBytes`: Byte length of generated export artifact.
- `contentHash`: SHA-256 digest string.
- `exportSuccess`: Boolean status flag.
- `exportTimestamp`: ISO-8601 UTC creation timestamp.

---

## 8. REST API Endpoints

### 1. Export Specific Format
- **Endpoint**: `POST /api/v1/production-assets/{id}/exports`
- **Request Body**:
  ```json
  {
    "exportType": "MARKDOWN"
  }
  ```
- **Response**: `200 OK` (`ExportResponse`)

### 2. Export Complete Production Package (ZIP)
- **Endpoint**: `POST /api/v1/production-assets/{id}/package`
- **Response**: `200 OK` (`ExportResponse` with `exportType: "PRODUCTION_PACKAGE"`)

### 3. List Export History for Asset
- **Endpoint**: `GET /api/v1/production-assets/{id}/exports`
- **Response**: `200 OK` (Paged `ExportResponse`)

### 4. Get Export Metadata
- **Endpoint**: `GET /api/v1/production-exports/{exportId}`
- **Response**: `200 OK` (`ExportResponse`)

### 5. Download Exported Binary Artifact
- **Endpoint**: `GET /api/v1/production-exports/{exportId}/download`
- **Response**: `200 OK` with binary body, content-disposition header, and `X-Content-Hash`.

---

## 9. Limitations & Boundary Scope

- Automatic publishing to YouTube, Instagram, X, TikTok, or Reddit is **strictly excluded**.
- Media generation (AI voice cloning, AI video synthesis, AI image rendering) is excluded.
- Content exports serve as creator deliverables for human review, recording, and distribution.
