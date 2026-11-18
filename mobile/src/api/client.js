import axios from 'axios';
import { URL_PADRAO } from '../config/servidor';

const api = axios.create({ baseURL: `${URL_PADRAO}/api`, timeout: 10000 });

// Muda o servidor usado em todos os pedidos (ex.: "http://10.0.97.201:8080")
export function definirServidor(urlBase) {
  api.defaults.baseURL = `${urlBase}/api`;
}

export function obterServidor() {
  return api.defaults.baseURL.replace(/\/api$/, '');
}

// Depois do login, todos os pedidos levam o token JWT no cabeçalho
export function definirToken(token) {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common.Authorization;
  }
}

// Transforma um erro do axios numa mensagem para mostrar ao utilizador.
// O backend devolve os erros no formato ErroResposta { estado, erro, ... }
export function mensagemDeErro(e, porDefeito = 'Ocorreu um erro.') {
  if (e?.response) {
    return e.response.data?.erro ?? porDefeito;
  }
  return 'Não foi possível ligar ao servidor. Verifica o endereço em "Servidor".';
}

export default api;
