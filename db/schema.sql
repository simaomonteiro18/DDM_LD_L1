CREATE TABLE utilizador (
    id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uq_utilizador_username ON utilizador (LOWER(username));

CREATE TABLE tipo_artigo (
    id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uq_tipo_artigo_nome ON tipo_artigo (LOWER(nome));

CREATE TABLE artigo (
    id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    id_tipo_artigo INTEGER NOT NULL REFERENCES tipo_artigo (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_artigo_tipo_nome ON artigo (id_tipo_artigo, LOWER(nome));

CREATE TABLE orcamento (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ano             INTEGER NOT NULL CHECK (ano BETWEEN 2000 AND 2100),
    mes             INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    valor           NUMERIC(10, 2) NOT NULL CHECK (valor >= 0),
    id_criado_por   INTEGER NOT NULL REFERENCES utilizador (id) ON DELETE RESTRICT,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_alterado_por INTEGER REFERENCES utilizador (id) ON DELETE RESTRICT,
    data_alterado   TIMESTAMPTZ,
    CONSTRAINT uq_orcamento_ano_mes UNIQUE (ano, mes),
    CONSTRAINT ck_orcamento_alteracao CHECK ((id_alterado_por IS NULL) = (data_alterado IS NULL))
);

CREATE TABLE compra (
    id              INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT now(),
    id_criado_por   INTEGER NOT NULL REFERENCES utilizador (id) ON DELETE RESTRICT,
    data_alterado   TIMESTAMPTZ,
    id_alterado_por INTEGER REFERENCES utilizador (id) ON DELETE RESTRICT,
    fechada         BOOLEAN NOT NULL DEFAULT FALSE,
    id_fechada_por  INTEGER REFERENCES utilizador (id) ON DELETE RESTRICT,
    data_fechada    TIMESTAMPTZ,
    CONSTRAINT ck_compra_alteracao CHECK ((id_alterado_por IS NULL) = (data_alterado IS NULL)),
    CONSTRAINT ck_compra_fecho CHECK (
        (NOT fechada AND id_fechada_por IS NULL AND data_fechada IS NULL)
        OR (fechada AND id_fechada_por IS NOT NULL AND data_fechada IS NOT NULL)
    )
);

CREATE TABLE item_compra (
    id                   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_compra            INTEGER NOT NULL REFERENCES compra (id) ON DELETE CASCADE,
    id_artigo            INTEGER NOT NULL REFERENCES artigo (id) ON DELETE RESTRICT,
    quantidade_adquirida INTEGER CHECK (quantidade_adquirida > 0),
    preco_unitario       NUMERIC(10, 2) CHECK (preco_unitario >= 0),
    CONSTRAINT ck_item_compra_adquirido CHECK ((quantidade_adquirida IS NULL) = (preco_unitario IS NULL))
);

CREATE TABLE item_compra_previsto (
    id                  INTEGER PRIMARY KEY REFERENCES item_compra (id) ON DELETE CASCADE,
    quantidade_prevista INTEGER NOT NULL CHECK (quantidade_prevista > 0)
);

CREATE TABLE item_compra_nao_previsto (
    id          INTEGER PRIMARY KEY REFERENCES item_compra (id) ON DELETE CASCADE,
    observacoes TEXT
);

CREATE INDEX idx_item_compra_compra ON item_compra (id_compra);
CREATE INDEX idx_item_compra_artigo ON item_compra (id_artigo);
CREATE INDEX idx_compra_fechada ON compra (fechada);

CREATE FUNCTION fn_item_previsto_valido() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM item_compra_nao_previsto WHERE id = NEW.id) THEN
        RAISE EXCEPTION 'O item ja esta registado como nao previsto';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_item_previsto_valido
BEFORE INSERT ON item_compra_previsto
FOR EACH ROW EXECUTE FUNCTION fn_item_previsto_valido();

CREATE FUNCTION fn_item_nao_previsto_valido() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM item_compra_previsto WHERE id = NEW.id) THEN
        RAISE EXCEPTION 'O item ja esta registado como previsto';
    END IF;
    IF (SELECT quantidade_adquirida FROM item_compra WHERE id = NEW.id) IS NULL THEN
        RAISE EXCEPTION 'Um item nao previsto tem de entrar ja adquirido';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_item_nao_previsto_valido
BEFORE INSERT ON item_compra_nao_previsto
FOR EACH ROW EXECUTE FUNCTION fn_item_nao_previsto_valido();
