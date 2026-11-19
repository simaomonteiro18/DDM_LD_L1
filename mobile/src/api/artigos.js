import api from './client';

// Pedidos ao controller de artigos do backend
// TODO: confirmar a rota e os campos no docs/api.md
const BASE = '/artigos';

export async function listarArtigos() {
  const res = await api.get(BASE);
  return res.data;
}

export async function criarArtigo(dados) {
  const res = await api.post(BASE, corpo(dados));
  return res.data;
}

export async function atualizarArtigo(id, dados) {
  const res = await api.put(`${BASE}/${id}`, corpo(dados));
  return res.data;
}

export async function apagarArtigo(id) {
  await api.delete(`${BASE}/${id}`);
}

// Corpo enviado ao backend. TODO: confirmar o nome do campo do tipo (tipoArtigoId? tipoId?)
function corpo({ nome, tipoId }) {
  return { nome, tipoArtigoId: tipoId };
}

// O tipo pode vir como id solto ou como objeto aninhado, conforme a resposta do backend
export function idDoTipo(artigo) {
  return artigo?.tipoArtigoId ?? artigo?.tipoId ?? artigo?.tipoArtigo?.id ?? artigo?.tipo?.id ?? null;
}

export function nomeDoTipo(artigo) {
  return artigo?.tipoArtigoNome ?? artigo?.tipoArtigo?.nome ?? artigo?.tipo?.nome ?? '';
}
