import AsyncStorage from '@react-native-async-storage/async-storage';

const CHAVE = 'ishopping.urlServidor';

// Endereço por defeito: vem do ficheiro mobile/.env.local (EXPO_PUBLIC_API_URL).
// Se não existir, usa o endereço do emulador Android para o PC.
export const URL_PADRAO = process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8080';

// Aceita "10.0.97.201:8080", "http://10.0.97.201:8080/" ou ".../api"
// e devolve sempre "http://10.0.97.201:8080"
export function normalizarUrl(texto) {
  let url = texto.trim().replace(/\/+$/, '');
  if (url.toLowerCase().endsWith('/api')) url = url.slice(0, -4);
  if (!/^https?:\/\//i.test(url)) url = `http://${url}`;
  return url;
}

export function urlValido(url) {
  return /^https?:\/\/[^\s/]+$/i.test(url);
}

// Endereço guardado no telemóvel (ou o por defeito, se nunca foi alterado)
export async function lerUrlGuardada() {
  try {
    return (await AsyncStorage.getItem(CHAVE)) ?? URL_PADRAO;
  } catch {
    return URL_PADRAO;
  }
}

export async function guardarUrl(url) {
  await AsyncStorage.setItem(CHAVE, url);
}

export async function reporUrl() {
  await AsyncStorage.removeItem(CHAVE);
}
