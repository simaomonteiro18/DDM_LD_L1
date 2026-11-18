import { createContext, useContext, useEffect, useState } from 'react';
import { Alert } from 'react-native';
import { definirToken, definirAoExpirarSessao } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Utilizador logado { id, nome, username }, guardado durante a execucao.
  // O token fica no cliente da API (definirToken).
  const [user, setUser] = useState(null);

  // A resposta do login e { token, tipo, expiraEm, utilizador: { id, nome, username } }
  function entrar(resposta) {
    definirToken(resposta.token);
    const { id, nome, username } = resposta.utilizador;
    setUser({ id, nome, username });
  }

  function sair() {
    definirToken(null);
    setUser(null);
  }

  // Um pedido autenticado devolveu 401: o token expirou ou o utilizador foi apagado.
  // O 401 do proprio login nao passa por aqui (ver o interceptor em api/client.js).
  useEffect(() => {
    definirAoExpirarSessao(() => {
      sair();
      Alert.alert('Sessão terminada', 'A sessão terminou. Entra de novo.');
    });
    return () => definirAoExpirarSessao(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, entrar, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
