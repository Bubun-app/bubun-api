-- ============================================
-- EXTENSIONS
-- ============================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUMS
-- ============================================

CREATE TYPE user_mode AS ENUM ('FULL', 'GROUP_ONLY');

CREATE TYPE contact_status AS ENUM ('PENDING', 'ACCEPTED');

CREATE TYPE account_source AS ENUM ('MANUAL', 'OPEN_BANKING');

CREATE TYPE account_type AS ENUM ('CASH', 'CURRENT',  'SAVINGS', 'CREDIT', 'INVESTMENT');

CREATE TYPE integration_provider AS ENUM ( 'GOCARDLESS' );

CREATE TYPE split_status AS ENUM ('PENDING', 'ACCEPTED', 'DISPUTED');

-- ============================================
-- 1. USERS & CONTACTS
-- ============================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    avatar_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    base_currency VARCHAR(3) NOT NULL,
    mode user_mode NOT NULL DEFAULT 'FULL',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE contacts (
    sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status contact_status NOT NULL DEFAULT 'PENDING',
    balance_from_sender DECIMAL(19,4) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (sender_user_id, receiver_user_id),
    CHECK (sender_user_id <> receiver_user_id)
);

-- ============================================
-- 2. CATEGORIES
-- ============================================

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    parent_id UUID REFERENCES categories(id),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================
-- 3. INSTITUTIONS
-- ============================================

CREATE TABLE institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gc_institution_id VARCHAR(100),
    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(250) NOT NULL
);

-- ============================================
-- 4. ACCOUNTS
-- ============================================

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    institution_id UUID REFERENCES institutions(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    source account_source NOT NULL DEFAULT 'MANUAL',
    type account_type NOT NULL DEFAULT 'CASH',
    currency VARCHAR(3) NOT NULL,
    personal_percentage DECIMAL(5,2) DEFAULT 100.00
        CHECK (personal_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN DEFAULT TRUE,
    current_balance DECIMAL(19,4) DEFAULT 0.00,
    CHECK (
        (source = 'OPEN_BANKING' AND institution_id IS NOT NULL)
        OR source = 'MANUAL'
    )
);

CREATE TABLE account_integrations (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
	provider integration_provider NOT NULL,
	external_account_id VARCHAR(255) NOT NULL,
	external_requisition_id VARCHAR(255),
	last_synced_at TIMESTAMPTZ,
	sync_status VARCHAR(20),
	created_at TIMESTAMPTZ DEFAULT NOW(),
	UNIQUE (provider, external_account_id)
);

-- ============================================
-- 5. ACCOUNT TRANSACTIONS
-- ============================================

CREATE TABLE account_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    amount DECIMAL(19,4) NOT NULL,
    gc_transaction_id VARCHAR(255) UNIQUE,
    gc_internal_id UUID UNIQUE,
    booking_datetime TIMESTAMPTZ,
    value_datetime TIMESTAMPTZ NOT NULL,
    remittance_info TEXT,
    creditor_name VARCHAR(255),
    debtor_name VARCHAR(255),
    is_manual BOOLEAN DEFAULT FALSE,
    transfer_transaction_id UUID REFERENCES account_transactions(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 6. USER EXPENSES
-- ============================================

CREATE TABLE user_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    category_id UUID REFERENCES categories(id),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    creation_timestamp_utc TIMESTAMPTZ NOT NULL,
    expense_timestamp_utc TIMESTAMPTZ NOT NULL,
    description TEXT,
    warranty_until DATE,
    receipt_url VARCHAR(500),
    exchange_rate DECIMAL(19,4),
    location_label VARCHAR(255),
    latitude DECIMAL(9,6),
	longitude DECIMAL(9,6),
	CHECK (
	    (latitude IS NULL AND longitude IS NULL)
	    OR (latitude IS NOT NULL AND longitude IS NOT NULL)
	)
);

-- ============================================
-- 7. GROUPS & MEMBERS
-- ============================================

CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    default_currency VARCHAR(3) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id)
);

CREATE TABLE group_members (
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.00,
    trust_mode_enabled BOOLEAN DEFAULT FALSE,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    PRIMARY KEY (group_id, user_id)
);

-- ============================================
-- 8. GROUP EXPENSES
-- ============================================

CREATE TABLE group_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    creator_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    exchange_rate DECIMAL(19,4),
    category_id UUID REFERENCES categories(id),
    amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    creation_timestamp_utc TIMESTAMPTZ NOT NULL,
    expense_timestamp_utc TIMESTAMPTZ NOT NULL,
    description TEXT,
    warranty_until DATE,
    receipt_url VARCHAR(500),
    location_label VARCHAR(255),
    latitude DECIMAL(9,6),
	longitude DECIMAL(9,6),
	CHECK (
	    (latitude IS NULL AND longitude IS NULL)
	    OR (latitude IS NOT NULL AND longitude IS NOT NULL)
	)
);

CREATE TABLE group_expense_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_expense_id UUID NOT NULL REFERENCES group_expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    amount_owed DECIMAL(19,4) DEFAULT 0.00 CHECK (amount_owed >= 0),
    amount_paid DECIMAL(19,4) DEFAULT 0.00 CHECK (amount_paid >= 0),
    status split_status NOT NULL DEFAULT 'PENDING',
    UNIQUE (group_expense_id, user_id)
);

-- ============================================
-- 9. INVITES & SETTLEMENTS
-- ============================================

CREATE TABLE group_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id),
    inviter_id UUID REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    max_uses INT DEFAULT 1,
    use_count INT DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    sender_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    sender_account_transaction_id UUID REFERENCES account_transactions(id),
    receiver_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    receiver_account_transaction_id UUID NOT NULL REFERENCES account_transactions(id),
    amount DECIMAL(19,4) CHECK (amount > 0),
    CHECK (sender_user_id <> receiver_user_id)
);

-- ============================================
-- 10. LINKING TABLES
-- ============================================

CREATE TABLE user_expense_account_transactions (
    user_expense_id UUID REFERENCES user_expenses(id) ON DELETE RESTRICT,
    account_transaction_id UUID REFERENCES account_transactions(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_expense_id, account_transaction_id)
);

CREATE TABLE user_expense_group_splits (
    user_expense_id UUID REFERENCES user_expenses(id) ON DELETE RESTRICT,
    group_expense_split_id UUID REFERENCES group_expense_splits(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_expense_id, group_expense_split_id)
);

-- ============================================
-- 11. TAGS
-- ============================================

CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL
);

CREATE TABLE user_expense_tags (
    user_expense_id UUID REFERENCES user_expenses(id),
    tag_id UUID REFERENCES tags(id),
    PRIMARY KEY (user_expense_id, tag_id)
);

CREATE TABLE group_expense_tags (
    group_expense_id UUID REFERENCES group_expenses(id),
    tag_id UUID REFERENCES tags(id),
    PRIMARY KEY (group_expense_id, tag_id)
);

-- ============================================
-- 12. ACTIVITY LOG
-- ============================================

CREATE TABLE activity_action_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL
);

CREATE TABLE activity_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action_type UUID REFERENCES activity_action_types(id) NOT NULL,
    entity_id UUID,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- INDEXES
-- ============================================

CREATE UNIQUE INDEX idx_users_email_unique
    ON users(email)
    WHERE email IS NOT NULL;

CREATE INDEX idx_contacts_sender ON contacts(sender_user_id);
CREATE INDEX idx_contacts_contacted ON contacts(receiver_user_id);

CREATE INDEX idx_categories_user ON categories(user_id);
CREATE INDEX idx_categories_parent ON categories(parent_id);

CREATE INDEX idx_accounts_user ON accounts(user_id);
CREATE INDEX idx_accounts_active ON accounts(user_id, is_active);

CREATE INDEX idx_account_tx_account_date
    ON account_transactions(account_id, value_datetime DESC);

CREATE INDEX idx_account_tx_booking
    ON account_transactions(account_id, booking_datetime);

CREATE INDEX idx_account_tx_transfer
    ON account_transactions(transfer_transaction_id);

CREATE INDEX idx_user_expenses_user_date
    ON user_expenses(user_id, expense_timestamp_utc DESC);

CREATE INDEX idx_user_expenses_category
    ON user_expenses(category_id);

CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_group_members_active ON group_members(group_id, left_at);

CREATE INDEX idx_group_expenses_group_date
    ON group_expenses(group_id, expense_timestamp_utc DESC);

CREATE INDEX idx_group_splits_user_status
    ON group_expense_splits(user_id, status);

CREATE INDEX idx_group_splits_expense
    ON group_expense_splits(group_expense_id);

CREATE INDEX idx_settlements_group ON settlements(group_id);
CREATE INDEX idx_settlements_sender ON settlements(sender_user_id);
CREATE INDEX idx_settlements_receiver ON settlements(receiver_user_id);

CREATE INDEX idx_activity_group_date
    ON activity_log(group_id, created_at DESC);