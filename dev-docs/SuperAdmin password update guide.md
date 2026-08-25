# 🔑 Super admin Password Update Guide

> **Does not apply to this fork as deployed.** `superadmin@test.com` is disabled
> (`enabled = false`), so the sign-in in step 1 fails by design and there is nothing to
> change the password of. Leave it disabled and use a regular admin account. Background and
> the reason the account must not be deleted: [`../CLAUDE.md`](../CLAUDE.md) →
> "Known upstream issue: the default super admin".

## 1️⃣ Sign In

Send a `POST` request to your authentication endpoint with the following credentials:

**Endpoint:** `/auth/signin`  
**Method:** `POST`  
**Headers:**
```json
{
  "Content-Type": "application/json"
}
```
**Body:**
```json
{
  "email": "superadmin@test.com",
  "password": "pls_change_me",
  "type": "SUPER_ADMIN"
}
```

📌 **Response (Example)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluQHRlc3QuY29tIiwiYXV0aCI6W3siYXV0aG9yaXR5IjoiUk9MRV9TVVBFUl9BRE1JTiJ9XSwiaWF0IjoxNzQxODc4NzMzLCJleHAiOjE3NDMwODgzMzN9.EualUl3tsR6beq1ENYgcdnYIWjFsw5WHdgnxvNBH2RU"
}
```
Save this `accessToken`, as you'll need it for authentication.

---

## 2️⃣ Change Password

To update your password, send a `POST` request to the `/updatepwd` endpoint.

**Endpoint:** `/auth/updatepwd`  
**Method:** `POST`  
**Headers:**
```json
{
  "Content-Type": "application/json",
  "Authorization": "Bearer YOUR_ACCESS_TOKEN"
}
```
**Body:**
```json
{
  "oldPassword": "pls_change_me",
  "newPassword": "YourNewSecurePassword"
}
```

📌 **Response (Success)**:
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```
