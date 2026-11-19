import api from './client';

// Pedidos ao controller de tipos de artigo do backend
// TODO: confirmar a rota no docs/api.md
const BASE = '/tipos-artigo';

export async function listarTiposArtigo() {
  const res = await api.get(BASE);
  return res.data;
}

export async function criarTipoArtigo(dados) {
  const res = await api.post(BASE, dados);
  return res.data;
}

export async function atualizarTipoArtigo(id, dados) {
  const res = await api.put(`${BASE}/${id}`, dados);
  return res.data;
}

export async function apagarTipoArtigo(id) {
  await api.delete(`${BASE}/${id}`);
}
