import api from './client';

// Pedidos ao controller de utilizadores do backend (docs/api.md, secao 4)
const BASE = '/utilizadores';

export async function listarUtilizadores() {
  const res = await api.get(BASE);
  return res.data;
}

export async function criarUtilizador(dados) {
  const res = await api.post(BASE, corpo(dados));
  return res.data;
}

export async function atualizarUtilizador(id, dados) {
  const res = await api.put(`${BASE}/${id}`, corpo(dados));
  return res.data;
}

export async function apagarUtilizador(id) {
  await api.delete(`${BASE}/${id}`);
}

// Corpo enviado ao backend: nome, username e password (UtilizadorCriarPedido / UtilizadorAtualizarPedido).
// Na edicao, sem password o backend mantem a palavra-passe atual.
function corpo({ nome, username, password }) {
  return password ? { nome, username, password } : { nome, username };
}
