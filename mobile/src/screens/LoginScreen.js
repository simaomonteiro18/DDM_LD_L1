import { useState } from 'react';
import { View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet } from 'react-native';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const { entrar } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [erro, setErro] = useState('');
  const [aCarregar, setACarregar] = useState(false);

  async function fazerLogin() {
    if (!username.trim() || !password) {
      setErro('Preenche o username e a palavra-passe.');
      return;
    }
    setErro('');
    setACarregar(true);
    try {
      const res = await api.post('/auth/login', { username: username.trim(), password });
      if (!res.data?.token) throw new Error('Resposta sem token');
      entrar(res.data);
    } catch (e) {
      if (e.response) {
        // O backend respondeu com erro (ex.: 401 credenciais erradas, 400 validação)
        setErro(e.response.data?.erro ?? 'Username ou palavra-passe inválidos.');
      } else {
        setErro('Não foi possível ligar ao servidor. Verifica a rede e o IP.');
      }
    } finally {
      setACarregar(false);
    }
  }

  return (
    <View style={s.ecra}>
      <Text style={s.titulo}>iShopping</Text>
      <Text style={s.subtitulo}>Orçamento e compras do agregado familiar</Text>

      <Text style={s.label}>Username</Text>
      <TextInput
        style={s.input}
        value={username}
        onChangeText={setUsername}
        autoCapitalize="none"
        autoCorrect={false}
      />

      <Text style={s.label}>Palavra-passe</Text>
      <TextInput
        style={s.input}
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        onSubmitEditing={fazerLogin}
      />

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <Pressable
        style={[s.botao, aCarregar && { opacity: 0.6 }]}
        onPress={fazerLogin}
        disabled={aCarregar}
      >
        {aCarregar ? <ActivityIndicator color="#fff" /> : <Text style={s.botaoTexto}>Entrar</Text>}
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: '#F4F5F1' },
  titulo: { fontSize: 32, fontWeight: '800', textAlign: 'center', color: '#17201C' },
  subtitulo: { fontSize: 15, textAlign: 'center', color: '#5B6660', marginTop: 6, marginBottom: 36 },
  label: { fontSize: 14, fontWeight: '700', marginBottom: 6, color: '#17201C' },
  input: {
    height: 52, borderWidth: 1.5, borderColor: '#C9CFC6', borderRadius: 12,
    paddingHorizontal: 16, fontSize: 16, backgroundColor: '#fff', marginBottom: 16,
  },
  erro: { color: '#B42318', backgroundColor: '#FDECEA', padding: 12, borderRadius: 10, marginBottom: 16 },
  botao: { height: 54, borderRadius: 14, backgroundColor: '#0B6E55', alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
});