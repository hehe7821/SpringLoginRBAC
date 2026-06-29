# Spring Auth Template

Spring Boot 4, PostgreSQL 18.4, Redis 8.8 기반 JWT 인증 템플릿입니다. MyBatis는 XML mapper 방식으로 SQL을 분리합니다.

## Stack

- Java 25
- Spring Boot 4.0.7
- Spring Security
- MyBatis
- PostgreSQL 18.4
- Redis 8.8
- Gmail SMTP
- Swagger/OpenAPI

## Run

SMTP 인증 메일을 사용하려면 프로젝트 루트에 `.env`를 만들고 Gmail 앱 비밀번호를 넣습니다.

```env
SMTP_USER_NAME=your.gmail@gmail.com
SMTP_PASSWORD=your_gmail_app_password

# Optional. Empty이면 SMTP_USER_NAME을 From 주소로 사용합니다.
EMAIL_VERIFICATION_FROM=
```

실행:

```bash
docker compose up -d --build
```

`postgres-18-data` volume이 이미 존재하는 경우 `/docker-entrypoint-initdb.d`의 초기화 SQL은 다시 실행되지 않습니다.
기존 개발 DB를 새 bigint schema로 다시 만들려면 필요한 데이터를 백업한 뒤 Postgres volume을 재생성하세요.

서비스:

```text
Backend:  http://localhost:8080
Postgres: localhost:5432
Redis:    localhost:6379
Swagger:  http://localhost:8080/swagger-ui/index.html
OpenAPI:  http://localhost:8080/v3/api-docs
```

기본 DB 접속 정보:

```text
database: spring_auth
username: spring_auth
password: spring_auth
```

## Environment

| Name | Description | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/spring_auth` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `spring_auth` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `spring_auth` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `SMTP_USER_NAME` | Gmail SMTP account | empty |
| `SMTP_PASSWORD` | Gmail app password | empty |
| `EMAIL_VERIFICATION_FROM` | Verification email sender | `SMTP_USER_NAME` |
| `JWT_SECRET` | JWT HMAC secret | development default |
| `JWT_ACCESS_TOKEN_VALIDITY_SECONDS` | Access token TTL | `900` |
| `JWT_REFRESH_TOKEN_VALIDITY_SECONDS` | Refresh token TTL | `1209600` |
| `EMAIL_VERIFICATION_CODE_TTL_SECONDS` | Email verification code TTL | `300` |
| `EMAIL_VERIFICATION_VERIFIED_TTL_SECONDS` | Email verified flag TTL | `1800` |

운영 환경에서는 반드시 `JWT_SECRET`, `SMTP_USER_NAME`, `SMTP_PASSWORD`를 외부 secret으로 주입해야 합니다.

## Auth Flow

### Signup

```text
1. POST /api/v1/auth/email/verification/request
   purpose = SIGNUP

2. POST /api/v1/auth/email/verification/confirm
   email + purpose + code

3. POST /api/v1/auth/signup
   email + password + displayName

4. Response
   accessToken + refreshToken
```

회원가입은 `SIGNUP` 이메일 인증 완료 Redis flag가 있어야 가능합니다. 가입 성공 후 해당 verified flag는 삭제됩니다.

### Login

```text
POST /api/v1/auth/login
```

이메일과 비밀번호가 맞으면 access token과 refresh token을 발급합니다. 비밀번호가 틀리거나 이메일이 없으면 동일하게 `401`을 반환합니다.

### Refresh

```text
POST /api/v1/auth/refresh
```

Redis에 저장된 refresh token과 요청 refresh token이 일치해야 새 token pair를 발급합니다.

### Logout

```text
POST /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

현재 사용자의 refresh token을 Redis에서 삭제합니다.

### Password Reset

```text
1. POST /api/v1/auth/email/verification/request
   purpose = PASSWORD_RESET

2. POST /api/v1/auth/email/verification/confirm
   email + purpose + code

3. POST /api/v1/auth/password/reset
   email + newPassword
```

비밀번호 재설정은 `PASSWORD_RESET` 이메일 인증 완료 Redis flag가 있어야 가능합니다. 변경 성공 후 해당 verified flag와 기존 refresh token은 삭제됩니다.

## API

### Public APIs

| Feature | Method | Path | Description |
| --- | --- | --- | --- |
| Email verification request | POST | `/api/v1/auth/email/verification/request` | Sends a 6-character code by Gmail SMTP |
| Email verification confirm | POST | `/api/v1/auth/email/verification/confirm` | Confirms code and stores verified flag in Redis |
| Signup | POST | `/api/v1/auth/signup` | Creates user after `SIGNUP` email verification |
| Login | POST | `/api/v1/auth/login` | Issues JWT token pair |
| JWT refresh | POST | `/api/v1/auth/refresh` | Rotates access/refresh token pair |
| Password reset | POST | `/api/v1/auth/password/reset` | Changes password after `PASSWORD_RESET` email verification |

### Authenticated APIs

All authenticated APIs require:

```http
Authorization: Bearer <accessToken>
```

| Feature | Method | Path | Description |
| --- | --- | --- | --- |
| Logout | POST | `/api/v1/auth/logout` | Deletes refresh token from Redis |
| Get my profile | GET | `/api/v1/users/me` | Returns current user profile |
| Update my profile | PATCH | `/api/v1/users/me` | Updates `displayName` |
| Withdraw | DELETE | `/api/v1/users/me` | Soft-deletes current user |

## Request Examples

### Email Verification Request

```json
{
  "email": "user@example.com",
  "purpose": "SIGNUP"
}
```

`purpose`:

```text
SIGNUP
PASSWORD_RESET
```

### Email Verification Confirm

```json
{
  "email": "user@example.com",
  "purpose": "SIGNUP",
  "code": "A1B2C3"
}
```

### Signup

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "displayName": "User Name"
}
```

### Login

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

### Refresh

```json
{
  "refreshToken": "<refreshToken>"
}
```

### Password Reset

```json
{
  "email": "user@example.com",
  "newPassword": "NewPassword123!"
}
```

### Update My Profile

```json
{
  "displayName": "Updated User"
}
```

## Redis Keys

| Key Pattern | Purpose | TTL |
| --- | --- | --- |
| `auth:refresh:{userId}` | Current refresh token | `JWT_REFRESH_TOKEN_VALIDITY_SECONDS` |
| `auth:email-verification:code:{purpose}:{email}` | Email verification code | `EMAIL_VERIFICATION_CODE_TTL_SECONDS` |
| `auth:email-verification:verified:{purpose}:{email}` | Email verified flag | `EMAIL_VERIFICATION_VERIFIED_TTL_SECONDS` |

## Database

PostgreSQL 확장:

```sql
create extension if not exists citext;
```

`citext`는 이메일 unique 처리를 대소문자 구분 없이 하기 위해 사용합니다.

### updated_at Trigger Function

```sql
create or replace function update_updated_at_column()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;
```

### users

| Column | Type | Description |
| --- | --- | --- |
| `user_id` | `bigint` | PK, generated always as identity |
| `email` | `citext` | Login email |
| `password_hash` | `text` | BCrypt password hash |
| `display_name` | `varchar(50)` | Display name |
| `status` | `varchar(20)` | `ACTIVE`, `PENDING`, `LOCKED`, `WITHDRAWN` |
| `email_verified_at` | `timestamptz` | Email verification timestamp |
| `last_login_at` | `timestamptz` | Last login timestamp |
| `password_changed_at` | `timestamptz` | Password changed timestamp |
| `created_at` | `timestamptz` | Created timestamp |
| `updated_at` | `timestamptz` | Updated timestamp |
| `deleted_at` | `timestamptz` | Soft delete timestamp |

Indexes and constraints:

```sql
constraint chk_users_status check (status in ('ACTIVE', 'PENDING', 'LOCKED', 'WITHDRAWN'))

create unique index uq_users_email_active
on users (email)
where deleted_at is null;

create index idx_users_status
on users (status);
```

Current behavior:

- Signup inserts `email`, `password_hash`, `display_name`; `user_id`, timestamps, and `status` are DB defaults.
- Withdraw sets `status = 'WITHDRAWN'` and `deleted_at = now()`.
- Queries ignore users where `deleted_at is not null`.

### roles

| Column | Type | Description |
| --- | --- | --- |
| `role_id` | `bigint` | PK, generated always as identity |
| `role_code` | `varchar(50)` | Unique role code, e.g. `USER`, `ADMIN` |
| `role_name` | `varchar(100)` | Role name |
| `description` | `text` | Description |
| `is_system` | `boolean` | System role flag |
| `is_active` | `boolean` | Active flag |
| `created_at` | `timestamptz` | Created timestamp |
| `updated_at` | `timestamptz` | Updated timestamp |

Constraints:

```sql
constraint uq_roles_role_code unique (role_code)
constraint chk_roles_role_code_format check (role_code ~ '^[A-Z][A-Z0-9_]*$')
```

### permissions

| Column | Type | Description |
| --- | --- | --- |
| `permission_id` | `bigint` | PK, generated always as identity |
| `permission_code` | `varchar(100)` | Unique permission code |
| `resource` | `varchar(50)` | Resource name |
| `action` | `varchar(50)` | Action name |
| `description` | `text` | Description |
| `is_active` | `boolean` | Active flag |
| `created_at` | `timestamptz` | Created timestamp |
| `updated_at` | `timestamptz` | Updated timestamp |

Constraints:

```sql
constraint uq_permissions_permission_code unique (permission_code)
constraint uq_permissions_resource_action unique (resource, action)
constraint chk_permissions_permission_code_format check (permission_code ~ '^[A-Z][A-Z0-9_]*$')
```

### user_roles

| Column | Type | Description |
| --- | --- | --- |
| `user_id` | `bigint` | FK to `users.user_id` |
| `role_id` | `bigint` | FK to `roles.role_id` |
| `assigned_at` | `timestamptz` | Assigned timestamp |
| `assigned_by` | `bigint` | FK to assigning user |
| `expires_at` | `timestamptz` | Optional expiration |
| `is_active` | `boolean` | Active assignment flag |

Primary key:

```sql
primary key (user_id, role_id)
```

Current behavior:

- Signup tries to assign role `USER` if an active `roles.role_code = 'USER'` row exists.
- If no `USER` role exists, signup still succeeds but user has no role authority.

### role_permissions

| Column | Type | Description |
| --- | --- | --- |
| `role_id` | `bigint` | FK to `roles.role_id` |
| `permission_id` | `bigint` | FK to `permissions.permission_id` |
| `granted_at` | `timestamptz` | Granted timestamp |
| `granted_by` | `bigint` | FK to granting user |

Primary key:

```sql
primary key (role_id, permission_id)
```

## Authorization Mapping

`UserMapper.xml` loads authorities as:

```text
ROLE_{role_code}
permission_code
```

Only active, non-expired role assignments are included.

## Timezone

Timestamp columns use `timestamptz` and DB-side `now()`. The stored value represents an absolute instant. API DTOs use `OffsetDateTime`; frontend clients should format timestamps for the user timezone, e.g. Asia/Seoul.

## Notes

- `user_id` is generated by PostgreSQL identity. The backend does not generate user IDs.
- Passwords are stored as BCrypt hashes.
- JWT is signed with HMAC-SHA256.
- Swagger is open in the current development configuration. Restrict it by profile or authentication before production use.
