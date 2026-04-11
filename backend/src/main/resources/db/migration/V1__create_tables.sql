-- V1: Create initial schema for personal budget tracker

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS categories (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    type    VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    user_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (name, user_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      BIGINT       NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    amount           NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    description      VARCHAR(255),
    transaction_date DATE         NOT NULL,
    type             VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_transactions_user_id       ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id   ON transactions (category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date          ON transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_type          ON transactions (type);
CREATE INDEX IF NOT EXISTS idx_categories_user_id         ON categories (user_id);
