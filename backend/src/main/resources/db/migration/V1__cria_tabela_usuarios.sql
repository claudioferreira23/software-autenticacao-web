CREATE TABLE usuarios (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(120)  NOT NULL,
    email       VARCHAR(254)  NOT NULL,
    senha_hash  VARCHAR(255)  NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_minusculo CHECK (email = lower(email))
);
