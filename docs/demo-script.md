## Demo Script

This demo shows how the project reproduces common web vulnerabilities, applies secure fixes, and verifies the results.

### 1. Project Entry

Open:

`http://localhost:8080/home`

This page serves as the main entry point of the lab and links to:

- file management
- comments
- audit logs

---

### 2. Insecure File Upload

Open:

`http://localhost:8080/files?user=alice`

#### Vulnerable idea

Weak upload validation may allow unsafe or unexpected file types.

#### Secure behavior demonstrated in this project

- only allowed file extensions are accepted
- oversized files are rejected

#### Demo steps

1. Open the file page as Alice.
2. Upload a valid file such as `.txt` or `.pdf`.
3. Confirm that the upload succeeds.
4. Try uploading a disallowed file type such as `.html` or `.zip`, or a file larger than 2MB.
5. Confirm that the system rejects the upload and shows a validation error.

---

### 3. Stored XSS

Open:

`http://localhost:8080/comments?user=alice`

#### Vulnerable idea

Unescaped stored content can execute JavaScript when other users view the page.

#### Secure behavior demonstrated in this project

Stored comment content is rendered safely as text.

#### Demo steps

1. Submit a comment containing:
   `<script>alert('XSS')</script>`
2. Reload the comments page.
3. Confirm that the script does not execute.
4. Confirm that the payload is displayed as plain text.

---

### 4. Broken Access Control: File Access (Horizontal Unauthorized Access)

#### Vulnerable endpoint

`http://localhost:8080/vuln/files/1?user=bob`

#### Secure endpoint

`http://localhost:8080/secure/files/1?user=bob`

#### Demo steps

1. Make sure file ID `1` belongs to Alice.
2. Access the vulnerable endpoint as Bob.
3. Confirm that Bob can read Alice’s file information.
4. Access the secure endpoint as Bob.
5. Confirm that the request is blocked with `403 Forbidden`.
6. Access the secure endpoint as Alice.
7. Confirm that the legitimate owner can still access the file.

---

### 5. Broken Access Control: Admin Audit Access (Vertical Privilege Escalation)

#### Vulnerable endpoint

`http://localhost:8080/vuln/admin/audit?user=bob`

#### Secure endpoint

`http://localhost:8080/secure/admin/audit?user=bob`

#### Demo steps

1. Access the vulnerable admin audit endpoint as Bob.
2. Confirm that a non-admin user can access admin-related audit information.
3. Access the secure admin audit endpoint as Bob.
4. Confirm that the request is blocked with `403 Forbidden`.
5. Access the secure admin audit endpoint as `testuser` (admin).
6. Confirm that admin access succeeds.

---

### 6. Security Events

Open:

`http://localhost:8080/security-events`

#### Demo steps

1. Trigger the vulnerable and secure file access scenarios.
2. Trigger the vulnerable and secure admin audit scenarios.
3. Open the Security Events page.
4. Confirm that actions such as the following are recorded:

- `VULN_FILE_ACCESS`
- `SECURE_FILE_ACCESS_DENIED`
- `SECURE_FILE_ACCESS_ALLOWED`
- `VULN_ADMIN_AUDIT_ACCESS`
- `SECURE_ADMIN_AUDIT_DENIED`
- `SECURE_ADMIN_AUDIT_ALLOWED`

---

### 7. Automated Verification

Run:

```powershell
.\mvnw.cmd -Dtest=AccessControlIntegrationTest test
```
