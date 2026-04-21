# LDAP Authentication Setup for Atlas CMMS

This document describes how to configure LDAP authentication with Atlas CMMS.

## Overview

Atlas CMMS supports LDAP authentication allowing users to log in using their existing LDAP/Active Directory credentials.
The system can:

- Authenticate users against your LDAP directory
- Automatically create users on first login
- Sync user details from LDAP
- Map organizational units (OUs) to roles

## ⚠️ Initial Setup Requirement (Important)

Before enabling LDAP authentication, you **must first create an organization admin account locally**.

### Step-by-step

1. **Start Atlas CMMS with LDAP disabled:**
   ```bash
   LDAP_ENABLED=false
   ```

2. **Create your first account manually**
    - Register through the UI
    - This account will become the **organization admin (owner)**

3. **Keep the email of this account**
    - Example: `admin@example.com`

4. **Enable LDAP**
    - Update your environment variables:
   ```bash
   LDAP_ENABLED=true
   LDAP_ORG_ADMIN=admin@example.com
   ```
Add other required LDAP variables as needed (e.g. `LDAP_URL`, `LDAP_BASE_DN`, etc.):

## Environment Variables

### Required Variables

| Variable                  | Description                              | Example                                        |
|---------------------------|------------------------------------------|------------------------------------------------|
| `LDAP_ENABLED`            | Enable LDAP authentication               | `true`                                         |
| `LDAP_ORG_ADMIN`          | Email of the organization admin          | `admin@example.com`                            |
| `LDAP_URL`                | LDAP server URL                          | `ldap://localhost:389`                         |
| `LDAP_BASE_DN`            | Base Distinguished Name                  | `dc=atlas,dc=com`                              |
| `LDAP_MANAGER_DN`         | Service account DN for LDAP binding      | cn=admin,dc=atlas,dc=com                       |
| `LDAP_MANAGER_PASSWORD`   | Service account password                 | adminPassword                                  |
| `LDAP_USER_SEARCH_BASES`  | OUs to search for users (pipe-separated) | `ou=engineering,ou=users\|ou=finance,ou=users` |
| `LDAP_USER_SEARCH_FILTER` | LDAP search filter                       | `(uid={0})`                                    |

### Optional Variables

| Variable                | Description                  | Default          |
|-------------------------|------------------------------|------------------|
| `LDAP_OU_ROLE_MAPPINGS` | OU to role mappings          | (none)           |
| `LDAP_SYNC_ENABLED`     | Enable user synchronization  | `true`           |
| `LDAP_SYNC_CREATE`      | Create new users from LDAP   | `true`           |
| `LDAP_SYNC_UPDATE`      | Update existing user details | `true`           |
| `LDAP_SYNC_DISABLE`     | Disable users not in LDAP    | `false`          |
| `LDAP_SYNC_CON`         | Cron schedule for sync       | `0 0 0,12 * * ?` |

### Attribute Mapping (Optional)

| Variable              | Description            | Default         |
|-----------------------|------------------------|-----------------|
| `LDAP_ATTR_EMAIL`     | Email attribute        | `mail`          |
| `LDAP_ATTR_FIRSTNAME` | First name attribute   | `givenName`     |
| `LDAP_ATTR_LASTNAME`  | Last name attribute    | `sn`            |
| `LDAP_ATTR_USERNAME`  | Username attribute     | `uid`           |
| `LDAP_OBJECT_CLASS`   | Object class for users | `inetOrgPerson` |

## Example Configuration

```bash
# Enable LDAP
LDAP_ENABLED=true

# Server configuration
LDAP_URL=ldap://localhost:389
LDAP_BASE_DN=dc=atlas,dc=com

# Service account (optional - for search mode)
LDAP_MANAGER_DN=cn=admin,dc=atlas,dc=com
LDAP_MANAGER_PASSWORD=adminpassword

# Organization admin (owner of the company)
LDAP_ORG_ADMIN=igc22@gmail.com

# User search configuration
LDAP_USER_SEARCH_BASES=ou=engineering,ou=users|ou=finance,ou=users
LDAP_USER_SEARCH_FILTER=(uid={0})

# OU to role mappings
LDAP_OU_ROLE_MAPPINGS=Backend=ADMIN|Finance=REQUESTER

# Sync settings
LDAP_SYNC_DISABLE=true
```

## Role Mappings

The `LDAP_OU_ROLE_MAPPINGS` variable maps LDAP organizational units to Atlas CMMS roles:

```
LDAP_OU_ROLE_MAPPINGS=Backend=ADMIN|Finance=REQUESTER
```

Format: `OU_NAME=ROLE_TYPE` (pipe-separated)

### Available Roles

- `ADMIN` - Full administrative access
- `LIMITED_ADMIN` - Limited administrative access
- `TECHNICIAN` - Can perform maintenance
- `LIMITED_TECHNICIAN` - Limited technician access
- `REQUESTER` - Can create work requests
- `VIEWER` - Read-only access

Example mappings:

```
Backend=ADMIN|Finance=REQUESTER|IT=ADMIN|Operations=TECHNICIAN|Contractors=LIMITED_TECHNICIAN
```

## How It Works

### Authentication Flow

1. User enters username and password on login page
2. Atlas attempts to bind to LDAP using the search bases and filter
3. On successful bind, user details are fetched from LDAP
4. If user doesn't exist in Atlas, they are created automatically
5. User is assigned a role based on their OU membership
6. JWT token is generated for session

### User Synchronization

When `LDAP_SYNC_ENABLED=true`:

- Scheduled job runs based on `LDAP_SYNC_CRON`
- Fetches all users from configured search bases
- Creates new users that exist in LDAP but not in Atlas (if `LDAP_SYNC_CREATE=true`)
- Updates existing user details (if `LDAP_SYNC_UPDATE=true`)
- Disables users not in LDAP (if `LDAP_SYNC_DISABLE=true`)

### Role Assignment

Roles are determined by the organizational unit:

1. On login, the user's DN is retrieved from LDAP
2. The OU(s) are extracted from the DN
3. First matching OU from `LDAP_OU_ROLE_MAPPINGS` determines the role
4. Default role (`LIMITED_TECHNICIAN`) is used if no match

## Security Considerations

1. **Use TLS**: Always use `ldaps://` in production
2. **Service Account**: Use a dedicated LDAP account with minimal permissions
3. **Passwords**: Store LDAP passwords in environment variables or secrets manager
