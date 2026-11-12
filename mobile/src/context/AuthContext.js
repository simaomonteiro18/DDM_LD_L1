import { createContext, useContext, useState } from 'react';
import { definirToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  // Dados do utilizador logado (inclui o token), guardados durante a execução
  const [user, setUser] = useState(null);

  function entrar(dados) {
    definirToken(dados.token);
    setUser(dados);
  }

  function sair() {
    definirToken(null);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, entrar, sair }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);