import axios from 'axios';

// IP do teu PC (vê com "ipconfig"); muda quando trocares de rede
export const API_URL = 'http://10.0.97.201:8080/api';

const api = axios.create({ baseURL: API_URL, timeout: 10000 });

// Depois do login, todos os pedidos levam o token JWT no cabeçalho
export function definirToken(token) {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common.Authorization;
  }
}

export default api;