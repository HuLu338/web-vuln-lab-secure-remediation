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
