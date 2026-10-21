INSERT INTO tipo_artigo (nome) VALUES
    ('Mercearia'),
    ('Laticínios'),
    ('Frutas e Legumes'),
    ('Carne e Peixe'),
    ('Limpeza'),
    ('Higiene');

INSERT INTO artigo (nome, id_tipo_artigo) VALUES
    ('Arroz', (SELECT id FROM tipo_artigo WHERE nome = 'Mercearia')),
    ('Massa', (SELECT id FROM tipo_artigo WHERE nome = 'Mercearia')),
    ('Azeite', (SELECT id FROM tipo_artigo WHERE nome = 'Mercearia')),
    ('Leite', (SELECT id FROM tipo_artigo WHERE nome = 'Laticínios')),
    ('Iogurte', (SELECT id FROM tipo_artigo WHERE nome = 'Laticínios')),
    ('Queijo', (SELECT id FROM tipo_artigo WHERE nome = 'Laticínios')),
    ('Maçã', (SELECT id FROM tipo_artigo WHERE nome = 'Frutas e Legumes')),
    ('Banana', (SELECT id FROM tipo_artigo WHERE nome = 'Frutas e Legumes')),
    ('Tomate', (SELECT id FROM tipo_artigo WHERE nome = 'Frutas e Legumes')),
    ('Frango', (SELECT id FROM tipo_artigo WHERE nome = 'Carne e Peixe')),
    ('Salmão', (SELECT id FROM tipo_artigo WHERE nome = 'Carne e Peixe')),
    ('Detergente da loiça', (SELECT id FROM tipo_artigo WHERE nome = 'Limpeza')),
    ('Lixívia', (SELECT id FROM tipo_artigo WHERE nome = 'Limpeza')),
    ('Champô', (SELECT id FROM tipo_artigo WHERE nome = 'Higiene')),
    ('Pasta de dentes', (SELECT id FROM tipo_artigo WHERE nome = 'Higiene'));
