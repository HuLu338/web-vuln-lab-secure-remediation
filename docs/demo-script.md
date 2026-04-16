# Demo Script

## 1. Start the application

- Run PostgreSQL
- Run Spring Boot
- Open `/home`

## 2. Show core features

- Open `/files?user=alice`
- Upload a file
- Download a file
- Delete a file

## 3. Demonstrate Broken Access Control (vulnerable version)

- Show that Alice only sees Alice's files
- Show that Bob only sees Bob's files
- Use Bob's file id with Alice
- Demonstrate unauthorized download/delete in the vulnerable version

## 4. Demonstrate Broken Access Control fix

- Repeat the same request
- Show `403 Forbidden`
- Show denied events in `/audit-logs`

## 5. Demonstrate Insecure File Upload

- Upload a normal `.txt` file
- Upload an unexpected file type in the vulnerable version
- Show that it was accepted

## 6. Demonstrate Insecure File Upload fix

- Show that unsupported types are rejected
- Show that oversized files are rejected

## 7. Demonstrate Stored XSS (vulnerable version)

- Open `/comments?user=alice`
- Submit `<script>alert('XSS')</script>`
- Show script execution
- Open `/comments?user=bob`
- Show that Bob is also affected

## 8. Demonstrate Stored XSS fix

- Show that the same input is displayed as plain text
- Show that the script is not executed

## 9. Show audit logs

- Open `/audit-logs`
- Show upload/download/delete logs
- Show denied access logs

## 10. Final summary

- This system demonstrates common web vulnerabilities
- It also shows secure remediation and auditability
