# Security Policy

## Security

The security of Atlas CMMS is very important. Since Atlas CMMS is often self-hosted and may manage sensitive operational data, security vulnerabilities are treated with high priority.

If you believe you have discovered a security vulnerability in Atlas CMMS, please report it responsibly as described below.

---

## Reporting a Security Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them privately by one of the following methods:

- Email: contact@atlascmms.com
- GitHub Security Advisory: https://github.com/grashjs/cmms/security/advisories/new

You should receive a response within **48 hours** acknowledging your report.

---

## What to Include in Your Report

Please include as much of the following information as possible:

- Type of vulnerability  
  (e.g., SQL injection, authentication bypass, XSS, privilege escalation)

- Affected component  
  (API, Web UI, Mobile App, Docker deployment, etc.)

- File paths or code locations involved

- Version of Atlas CMMS affected

- Step-by-step instructions to reproduce the issue

- Proof of concept (PoC) if possible

- Potential impact and attack scenario

This information helps us reproduce and fix the issue faster.

---

## Supported Versions

We typically provide security fixes for the latest stable release.

| Version | Supported |
|--------|-----------|
| Latest | ✅ |
| Older releases | ⚠️ Best effort |

---

## Responsible Disclosure

Please allow time for the issue to be fixed before publicly disclosing it.

We follow **responsible disclosure practices** and will:

- Acknowledge your report
- Investigate and validate the issue
- Provide a fix as quickly as possible
- Credit you in the security advisory if desired

---

## Security Best Practices for Self-Hosting

When running Atlas CMMS:

- Keep your Docker images up to date
- Use strong database passwords
- Run behind a reverse proxy (Traefik, Nginx, etc.)
- Enable HTTPS
- Restrict public access to admin interfaces

---

Thank you for helping make Atlas CMMS more secure.