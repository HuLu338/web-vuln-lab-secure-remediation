# Vulnerability Matrix

## Broken Access Control - Insecure Direct Object Reference

### Vulnerable Endpoints

- GET /files/download/{id}
- POST /files/delete/{id}

### Root Cause

The backend only checks the file id and does not verify whether the current user is the owner of the file.

### Impact

A user can download or delete another user's file by modifying the file id.

### Reproduction

1. Log in or simulate as alice.
2. Find a file id belonging to bob.
3. Access /files/download/{bobFileId} directly.
4. Observe that alice can download bob's file.
5. Send POST /files/delete/{bobFileId}.
6. Observe that alice can delete bob's file.

## Broken Access Control - Unauthorized File Deletion

### Vulnerable Endpoint

- POST /files/delete/{id}

### Root Cause

The backend deletes the file only by file id and does not verify whether the current user owns the file.

### Impact

A user can delete another user's file by sending a crafted request with another user's file id.

### Reproduction

1. Open `/files?user=bob` and find Bob's file id.
2. Open `/files?user=alice`.
3. Send a POST request to `/files/delete/{bobFileId}` with `user=alice`.
4. Observe that Bob's file is deleted from both the database and disk.

## Remediation

### Fix

The backend now verifies that the current user is the owner of the target file before allowing download or deletion.

### Result

- Unauthorized download now returns 403 Forbidden.
- Unauthorized deletion now returns 403 Forbidden.
- Users can only access their own files.

## Insecure File Upload

### Vulnerable Endpoint

- POST /files/upload

### Root Cause

The backend accepts uploaded files without enforcing a strict allowlist of file extensions or content types.

### Impact

Users can upload unexpected file types that should not be accepted by the system.

### Reproduction

1. Open `/files?user=alice`.
2. Upload a file with an unexpected type, such as `.html` or `.zip`.
3. Observe that the system accepts the file, stores it on disk, and saves a record in the database.

## Insecure File Upload - Remediation

### Fix

The backend now validates uploaded files using:

- an extension allowlist
- a maximum file size limit
- invalid filename checks

### Result

- Unsupported file types are rejected
- oversized files are rejected
- only approved file types are stored in the system

## Stored XSS

### Vulnerable Endpoint

- POST /comments
- GET /comments

### Root Cause

The application renders stored comment content using unescaped output.

### Impact

An attacker can store malicious script content in a comment, and the script will execute when other users view the comments page.

### Reproduction

1. Open `/comments?user=alice`.
2. Submit a comment containing `<script>alert('XSS')</script>`.
3. Reload the comments page or open `/comments?user=bob`.
4. Observe that the browser executes the stored script.

## Stored XSS - Remediation

### Fix

The application now renders comment content using escaped output instead of unescaped HTML rendering.

### Additional Protection

- empty comments are rejected
- overly long comments are rejected

### Result

Stored script content is displayed as plain text and is no longer executed by the browser.
