create extension if not exists citext;

create or replace function update_updated_at_column()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create table if not exists users (
    user_id bigint primary key generated always as identity,
    email citext not null,
    display_name varchar(50),
    status varchar(20) not null default 'ACTIVE',
    email_verified_at timestamptz,
    last_login_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint chk_users_status
        check (status in ('ACTIVE', 'PENDING', 'LOCKED', 'WITHDRAWN'))
);

create unique index if not exists uq_users_email_active
on users (email)
where deleted_at is null;

create index if not exists idx_users_status
on users (status);

drop trigger if exists trg_users_updated_at on users;
create trigger trg_users_updated_at
before update on users
for each row
execute function update_updated_at_column();

create table if not exists user_credentials (
    user_id bigint primary key,
    password_hash text not null,
    password_changed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint fk_user_credentials_user
        foreign key (user_id)
        references users (user_id)
        on delete cascade
);

drop trigger if exists trg_user_credentials_updated_at on user_credentials;
create trigger trg_user_credentials_updated_at
before update on user_credentials
for each row
execute function update_updated_at_column();

create table if not exists oauth_accounts (
    oauth_account_id bigint primary key generated always as identity,
    user_id bigint not null,
    provider varchar(30) not null,
    provider_user_id text not null,
    email citext,
    email_verified boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz,
    constraint fk_oauth_accounts_user
        foreign key (user_id)
        references users (user_id)
        on delete cascade,
    constraint chk_oauth_provider
        check (provider in ('GOOGLE'))
);

create unique index if not exists uq_oauth_provider_user_active
on oauth_accounts (provider, provider_user_id)
where deleted_at is null;

create unique index if not exists uq_oauth_user_provider_active
on oauth_accounts (user_id, provider)
where deleted_at is null;

create index if not exists idx_oauth_accounts_user_id
on oauth_accounts (user_id);

create index if not exists idx_oauth_accounts_provider_email
on oauth_accounts (provider, email)
where deleted_at is null;

drop trigger if exists trg_oauth_accounts_updated_at on oauth_accounts;
create trigger trg_oauth_accounts_updated_at
before update on oauth_accounts
for each row
execute function update_updated_at_column();

create table if not exists roles (
    role_id bigint primary key generated always as identity,
    role_code varchar(50) not null,
    role_name varchar(100) not null,
    description text,
    is_system boolean not null default false,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_roles_role_code unique (role_code),
    constraint chk_roles_role_code_format
        check (role_code ~ '^[A-Z][A-Z0-9_]*$')
);

create index if not exists idx_roles_is_active
on roles (is_active);

drop trigger if exists trg_roles_updated_at on roles;
create trigger trg_roles_updated_at
before update on roles
for each row
execute function update_updated_at_column();

create table if not exists permissions (
    permission_id bigint primary key generated always as identity,
    permission_code varchar(100) not null,
    resource varchar(50) not null,
    action varchar(50) not null,
    description text,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_permissions_permission_code unique (permission_code),
    constraint uq_permissions_resource_action unique (resource, action),
    constraint chk_permissions_permission_code_format
        check (permission_code ~ '^[A-Z][A-Z0-9_]*$')
);

create index if not exists idx_permissions_is_active
on permissions (is_active);

drop trigger if exists trg_permissions_updated_at on permissions;
create trigger trg_permissions_updated_at
before update on permissions
for each row
execute function update_updated_at_column();

create table if not exists user_roles (
    user_id bigint not null,
    role_id bigint not null,
    assigned_at timestamptz not null default now(),
    assigned_by bigint,
    expires_at timestamptz,
    is_active boolean not null default true,
    primary key (user_id, role_id),
    constraint fk_user_roles_user
        foreign key (user_id)
        references users (user_id)
        on delete cascade,
    constraint fk_user_roles_role
        foreign key (role_id)
        references roles (role_id)
        on delete restrict,
    constraint fk_user_roles_assigned_by
        foreign key (assigned_by)
        references users (user_id)
        on delete set null
);

create index if not exists idx_user_roles_role_id
on user_roles (role_id);

create index if not exists idx_user_roles_active
on user_roles (user_id, is_active);

create table if not exists role_permissions (
    role_id bigint not null,
    permission_id bigint not null,
    granted_at timestamptz not null default now(),
    granted_by bigint,
    primary key (role_id, permission_id),
    constraint fk_role_permissions_role
        foreign key (role_id)
        references roles (role_id)
        on delete cascade,
    constraint fk_role_permissions_permission
        foreign key (permission_id)
        references permissions (permission_id)
        on delete cascade,
    constraint fk_role_permissions_granted_by
        foreign key (granted_by)
        references users (user_id)
        on delete set null
);

create index if not exists idx_role_permissions_permission_id
on role_permissions (permission_id);

insert into roles (role_code, role_name, description, is_system, is_active)
values ('USER', 'User', 'Default application user role', true, true)
on conflict (role_code) do nothing;
