import { createContext, useContext, useEffect, useState } from 'react';
import api, { definirToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Utilizador logado { id, nome, username }: o Id fica guardado durante a execução
  const [user, setUser] = useState(null);

  // A resposta do login é { token, tipo, expiraEm, utilizador: { id, nome, username } }
  function entrar(resposta) {
    definirToken(resposta.token);
    setUser(resposta.utilizador);
  }

  function sair() {
    definirToken(null);
    setUser(null);
  }

  // Se o token expirar (8 horas) ou deixar de ser válido, o backend responde 401:
  // nesse caso termina a sessão e a app volta ao Login
  useEffect(() => {
    const id = api.interceptors.response.use(
      (resposta) => resposta,
      (erro) => {
        const ehLogin = erro.config?.url?.includes('/auth/login');
        if (erro.response?.status === 401 && !ehLogin) {
          sair();
        }
        return Promise.reject(erro);
      },
    );
    return () => api.interceptors.response.eject(id);
  }, []);

  return (
    <AuthContext.Provider value={{ user, entrar, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);