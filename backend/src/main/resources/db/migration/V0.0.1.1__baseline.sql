CREATE TABLE cluster
(
    id                  UUID         NOT NULL DEFAULT uuid_generate_v4(),
    name                VARCHAR(255) NOT NULL,
    bootstrap_servers   VARCHAR(255) NOT NULL,
    properties          hstore        NOT NULL,
    PRIMARY KEY (id)
);