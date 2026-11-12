import axios from 'axios';

// O telemóvel NÃO consegue usar "localhost": põe o IP do teu PC (vê com "ipconfig")
export const API_URL = 'http://10.0.97.201:8080/api';

const api = axios.create({ baseURL: API_URL, timeout: 10000 });

export default api;