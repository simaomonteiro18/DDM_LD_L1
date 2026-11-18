import api from './client';

// Pedidos ao OrcamentoController do backend (/api/orcamentos)
const BASE = '/orcamentos';

export async function listarOrcamentos() {
  const res = await api.get(BASE);
  return res.data;
}

export async function criarOrcamento(dados) {
  const res = await api.post(BASE, dados);
  return res.data;
}

export async function atualizarOrcamento(id, dados) {
  const res = await api.put(`${BASE}/${id}`, dados);
  return res.data;
}

export async function apagarOrcamento(id) {
  await api.delete(`${BASE}/${id}`);
}
