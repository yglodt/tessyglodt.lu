# Security Audit Report
**Date:** 2025-11-22
**Application:** tessyglodt.lu
**Technology Stack:** Spring Boot 3.3.0, Java 17, PostgreSQL, Thymeleaf

---

## Executive Summary

This security audit identified **8 critical security vulnerabilities** and **4 medium-severity issues** in the tessyglodt.lu application. The most severe issues include SQL injection vulnerabilities, stored Cross-Site Scripting (XSS), and hardcoded credentials in version control.

**Immediate Action Required:**
1. Fix SQL injection vulnerability in PageService.java
2. Remove hardcoded credentials from application.properties
3. Fix XSS vulnerabilities in templates
4. Implement password encoder for authentication

---

## Critical Vulnerabilities

### 1. SQL Injection in PageService.getPageByProperty() ⚠️ CRITICAL

**Location:** `src/main/java/lu/tessyglodt/site/service/PageService.java:116`

**Issue:**
```java
final String sql = "select p.*, d.id as dist_id, "
    + "d.name as dist_name, c.id as can_id, "
    + "c.name as can_name, m.id as mun_id, "
    + "m.name as mun_name from page p "
    + "left join municipality m on m.id = p.municipality "
    + "left join canton c on c.id = m.canton "
    + "left join district d on d.id = c.district where p." + property
    + " = ?";
```

The `property` parameter is concatenated directly into the SQL query without validation, allowing SQL injection attacks.

**Impact:** An attacker can execute arbitrary SQL commands, potentially:
- Extracting all database data
- Modifying or deleting data
- Bypassing authentication
- Executing administrative operations

**Proof of Concept:**
```java
// Attacker could call:
getPageByProperty("name = '1' OR '1'='1'; DROP TABLE page; --", "value", false)
```

**Recommendation:**
- Use a whitelist of allowed properties
- Validate the `property` parameter against allowed values only
- Example fix:
```java
private static final Set<String> ALLOWED_PROPERTIES = Set.of("id", "name", "title");

public Page getPageByProperty(final String property, final String value, final boolean log) {
    if (!ALLOWED_PROPERTIES.contains(property)) {
        throw new IllegalArgumentException("Invalid property: " + property);
    }
    // ... rest of the method
}
```

---

### 2. Potential SQL Injection in PageService.getSearchH2() ⚠️ CRITICAL

**Location:** `src/main/java/lu/tessyglodt/site/service/PageService.java:147`

**Issue:**
```java
params2 = params2.substring(0, params.length() - 1);
return getPagesWithWhere("where p.id in (" + params2 + ") order by title asc", null, false);
```

While the comment claims it's safe, building SQL queries with string concatenation is dangerous. The IDs come from the FT_SEARCH_DATA function, but this approach is fragile.

**Recommendation:**
- Use JdbcTemplate's IN clause support with arrays
- Or use a more structured approach with proper parameterization

---

### 3. Hardcoded Database Credentials ⚠️ CRITICAL

**Location:** `src/main/resources/application.properties:17-18`

**Issue:**
```properties
spring.datasource.username=tessyglodt
spring.datasource.password=tessyglodt
```

Database credentials are hardcoded in a file committed to version control.

**Impact:**
- Anyone with repository access has database credentials
- Credentials are visible in git history
- Cannot easily rotate credentials per environment

**Recommendation:**
- Move credentials to environment variables
- Use Spring's profile-specific properties (application-{profile}.properties)
- Add application.properties to .gitignore
- Use a secrets management solution (e.g., HashiCorp Vault, AWS Secrets Manager)
- Example:
```properties
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

---

### 4. Hardcoded Admin Credentials ⚠️ CRITICAL

**Location:** `src/main/resources/application.properties:32-34`

**Issue:**
```properties
spring.security.user.name=admin
spring.security.user.password=password
```

Admin credentials with a weak password ("password") are hardcoded in version control.

**Impact:**
- Trivial to compromise admin account
- All environments use the same credentials
- Credentials are in git history

**Recommendation:**
- Remove from application.properties immediately
- Use environment variables
- Implement proper user management with database-backed authentication
- Use strong passwords with a password encoder

---

### 5. Hardcoded API Keys ⚠️ CRITICAL

**Location:** `src/main/resources/application.properties:36-39`

**Issue:**
```properties
app.twitter.consumer-key=secret
app.twitter.consumer-secret=secret
app.twitter.access-token=secret
app.twitter.access-token-secret=secret
```

Twitter API credentials are in version control (even if currently set to "secret").

**Recommendation:**
- Move to environment variables
- Rotate all API keys immediately
- Add to .gitignore

---

### 6. Stored Cross-Site Scripting (XSS) ⚠️ CRITICAL

**Locations:**
- `src/main/resources/templates/page.html:10`
- `src/main/resources/templates/admin/pageform.html:64`
- `src/main/resources/templates/fragments/randomPage.html:5`

**Issue:**
```html
<!-- page.html:10 -->
<div th:utext="${page.content}" th:remove="tag"></div>

<!-- pageform.html:64 -->
<textarea name="content" id="content" class="form-control" th:errorclass="errors" th:utext="${page.content}"></textarea>

<!-- randomPage.html:5 -->
<a th:href="@{/page/{page}(page=${randomPage.name})}" th:utext="${#strings.substring(randomPage.content, 0, 450)}"></a>
```

The use of `th:utext` (unescaped text) allows HTML rendering without sanitization.

**Impact:**
- Stored XSS attacks
- Session hijacking
- Credential theft
- Malware distribution
- Defacement

**Recommendation:**
- Change `th:utext` to `th:text` for safe HTML escaping
- If HTML content is required, implement a strict Content Security Policy
- Use a HTML sanitization library (e.g., OWASP Java HTML Sanitizer)
- Example:
```html
<!-- Safe rendering -->
<div th:text="${page.content}" th:remove="tag"></div>
```

---

### 7. CKEditor Content Filter Disabled ⚠️ CRITICAL

**Location:** `src/main/resources/templates/admin/pageform.html:82`

**Issue:**
```javascript
CKEDITOR.replace("content", {
    extraPlugins : "autogrow",
    allowedContent : true  // ← Disables Advanced Content Filter
});
```

The `allowedContent: true` setting disables CKEditor's Advanced Content Filter (ACF), allowing all HTML content including dangerous scripts.

**Impact:**
- Makes stored XSS attacks trivial
- Allows admin users to accidentally or maliciously inject scripts
- No protection against malicious HTML

**Recommendation:**
- Remove `allowedContent: true`
- Configure a strict ACF whitelist:
```javascript
CKEDITOR.replace("content", {
    extraPlugins : "autogrow",
    allowedContent: 'p h1 h2 h3 h4 ul ol li strong em a[!href]; img[!src,alt,width,height]'
});
```

---

### 8. Exposed Google Maps API Key ⚠️ MEDIUM-HIGH

**Locations:**
- `src/main/resources/templates/page.html:57`
- `src/main/resources/templates/map.html:20`

**Issue:**
```javascript
script.src = "//maps.googleapis.com/maps/api/js?key=AIzaSyCJneX2_ynuWwRZthXVC_sf4AC5j4V5nqA&sensor=false&callback=showMap";
```

Google Maps API key is hardcoded in templates and visible to all users.

**Impact:**
- API key can be extracted and abused
- Potential for quota exhaustion
- Unauthorized usage charges

**Recommendation:**
- Use HTTP referrer restrictions in Google Cloud Console
- Consider using a backend proxy for API calls
- Rotate the API key
- Monitor API usage

---

## High Severity Issues

### 9. No Password Encoder Configured ⚠️ HIGH

**Location:** `src/main/java/lu/tessyglodt/site/spring/ConfigWebSecurity.java`

**Issue:**
No password encoder bean is defined in the security configuration.

**Impact:**
- Spring Security may use deprecated password encoding
- Passwords may not be properly hashed
- Vulnerable to password attacks

**Recommendation:**
Add a password encoder bean:
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

### 10. Missing HTTP Security Headers ⚠️ HIGH

**Location:** `src/main/java/lu/tessyglodt/site/spring/ConfigWebSecurity.java`

**Issue:**
No HTTP security headers are configured (X-Frame-Options, X-Content-Type-Options, Content-Security-Policy, etc.).

**Impact:**
- Vulnerable to clickjacking attacks
- MIME sniffing attacks possible
- No defense against XSS via CSP

**Recommendation:**
Add security headers configuration:
```java
http.headers()
    .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline' maps.googleapis.com; img-src 'self' data: maps.googleapis.com")
    .and()
    .frameOptions().deny()
    .and()
    .xssProtection().block(true)
    .and()
    .contentTypeOptions();
```

---

## Medium Severity Issues

### 11. Potential Open Redirect ⚠️ MEDIUM

**Location:** `src/main/java/lu/tessyglodt/site/spring/ConfigWebSecurity.java:53-57`

**Issue:**
```java
if (request.getServerName().contains("tessyglodt.lu")) {
    response.sendRedirect("https://www.tessyglodt.lu/");
} else {
    response.sendRedirect("/");
}
```

The `contains()` check can be bypassed with domains like `tessyglodt.lu.evil.com`.

**Impact:**
- Phishing attacks
- Credential theft

**Recommendation:**
Use exact domain matching:
```java
if ("tessyglodt.lu".equals(request.getServerName()) ||
    "www.tessyglodt.lu".equals(request.getServerName())) {
    response.sendRedirect("https://www.tessyglodt.lu/");
} else {
    response.sendRedirect("/");
}
```

---

### 12. Insufficient .gitignore Configuration ⚠️ MEDIUM

**Location:** `.gitignore`

**Issue:**
The .gitignore file only contains:
```
target/
```

**Impact:**
- Sensitive configuration files can be accidentally committed
- IDE files pollute the repository

**Recommendation:**
Add comprehensive .gitignore:
```
target/
.idea/
.vscode/
*.iml
.DS_Store
application-*.properties
!application-example.properties
*.log
```

---

### 13. Session Timeout Configuration ⚠️ MEDIUM

**Location:** `src/main/resources/application.properties:5`

**Issue:**
```properties
server.session-timeout=3600
```

One-hour session timeout may be too long for an admin interface.

**Recommendation:**
- Reduce session timeout to 15-30 minutes for admin users
- Implement proper session management with idle timeout
- Use `server.servlet.session.timeout=15m`

---

### 14. No HTTPS Enforcement ⚠️ MEDIUM

**Location:** `src/main/java/lu/tessyglodt/site/spring/ConfigWebSecurity.java:38-39`

**Issue:**
HTTPS enforcement is commented out:
```java
// http.authorizeRequests().and().requiresChannel().antMatchers("/login",
// "/authcheck", "/admin/**").requiresSecure();
```

**Recommendation:**
Enable HTTPS for sensitive endpoints:
```java
http.requiresChannel()
    .requestMatchers("/login", "/authcheck", "/admin/**")
    .requiresSecure();
```

---

## Low Severity Issues

### 15. Debug Logging Enabled ⚠️ LOW

**Location:** `src/main/resources/application.properties:30`

**Issue:**
```properties
logging.level.lu.tessyglodt.site=DEBUG
```

Debug logging in production can expose sensitive information.

**Recommendation:**
- Use INFO or WARN level in production
- Configure via environment-specific properties

---

### 16. Twitter Debug Enabled ⚠️ LOW

**Location:** `src/main/java/lu/tessyglodt/site/TwitterTemplateCreator.java:32`

**Issue:**
```java
cb.setDebugEnabled(true)
```

Debug mode enabled for Twitter API client.

**Recommendation:**
Disable debug mode or make it configurable.

---

## Positive Security Findings ✅

1. **CSRF Protection:** Enabled by default in Spring Security (not explicitly disabled)
2. **Parameterized Queries:** Most database queries use proper parameterization
3. **Spring Security:** Using Spring Security framework for authentication
4. **Input Validation:** Form validation implemented for admin forms
5. **Bot Detection:** User-agent based bot detection for view counting

---

## Recommendations Priority Matrix

| Priority | Issue | Effort | Impact |
|----------|-------|--------|--------|
| P0 (Immediate) | SQL Injection in getPageByProperty() | Low | Critical |
| P0 (Immediate) | Remove hardcoded credentials | Low | Critical |
| P0 (Immediate) | Fix XSS vulnerabilities | Low | Critical |
| P1 (This Week) | Implement password encoder | Low | High |
| P1 (This Week) | Configure security headers | Low | High |
| P1 (This Week) | Fix CKEditor ACF | Low | Critical |
| P2 (This Month) | Rotate all API keys | Medium | High |
| P2 (This Month) | Implement secrets management | Medium | High |
| P3 (Backlog) | Fix open redirect | Low | Medium |
| P3 (Backlog) | Update .gitignore | Low | Low |

---

## Compliance Considerations

### OWASP Top 10 (2021) Violations:

1. **A01:2021 - Broken Access Control** - Hardcoded admin credentials
2. **A02:2021 - Cryptographic Failures** - Credentials in version control
3. **A03:2021 - Injection** - SQL injection vulnerabilities
4. **A05:2021 - Security Misconfiguration** - Multiple configuration issues
5. **A07:2021 - XSS** - Stored XSS vulnerabilities

### GDPR Considerations:

- Hardcoded credentials violate Article 32 (Security of processing)
- No encryption of credentials in transit (no HTTPS enforcement)

---

## Remediation Roadmap

### Phase 1: Critical Fixes (Week 1)
1. Fix SQL injection vulnerability with property whitelist
2. Move all credentials to environment variables
3. Change `th:utext` to `th:text` in templates
4. Configure CKEditor ACF properly
5. Rotate all API keys and passwords

### Phase 2: Security Hardening (Week 2-3)
1. Implement password encoder
2. Configure HTTP security headers
3. Enable HTTPS enforcement
4. Fix open redirect vulnerability
5. Update .gitignore and remove sensitive files from git history

### Phase 3: Long-term Security (Month 2)
1. Implement secrets management solution
2. Set up security scanning in CI/CD
3. Implement Content Security Policy
4. Add security audit logging
5. Conduct penetration testing

---

## Testing Recommendations

1. **Security Testing:**
   - SQL injection testing on all database queries
   - XSS testing on all user input fields
   - Authentication testing with various credentials

2. **Automated Scanning:**
   - OWASP ZAP or Burp Suite for web vulnerability scanning
   - SonarQube for static code analysis
   - Dependency-Check for vulnerable dependencies

3. **Penetration Testing:**
   - Engage security professionals for comprehensive testing
   - Test in production-like environment

---

## Conclusion

The tessyglodt.lu application has **critical security vulnerabilities** that require immediate attention. The SQL injection vulnerability and hardcoded credentials pose the highest risk and should be addressed urgently.

Most issues can be resolved with low effort but have high security impact. Following the remediation roadmap will significantly improve the application's security posture.

**Next Steps:**
1. Review this report with the development team
2. Prioritize fixes according to the priority matrix
3. Implement Phase 1 critical fixes immediately
4. Schedule follow-up security audit after remediation

---

**Report Prepared By:** Claude (AI Security Auditor)
**Date:** 2025-11-22
