import { useCallback, useState } from 'react';
import { View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import api, { mensagemDeErro, obterServidor } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { cores } from '../tema';

export default function LoginScreen({ navigation }) {
  const { entrar } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [erro, setErro] = useState('');
  const [aCarregar, setACarregar] = useState(false);
  const [servidor, setServidor] = useState(obterServidor());

  // Atualiza o endereço mostrado quando se volta do ecrã "Servidor"
  useFocusEffect(useCallback(() => { setServidor(obterServidor()); }, []));

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
      setErro(mensagemDeErro(e, 'Username ou palavra-passe inválidos.'));
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

      <Pressable style={s.servidor} onPress={() => navigation.navigate('Servidor')}>
        <Text style={s.servidorTexto}>Servidor: {servidor}</Text>
        <Text style={s.servidorLink}>Alterar</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, justifyContent: 'center', padding: 24, backgroundColor: cores.fundo },
  titulo: { fontSize: 32, fontWeight: '800', textAlign: 'center', color: cores.texto },
  subtitulo: { fontSize: 15, textAlign: 'center', color: cores.textoSecundario, marginTop: 6, marginBottom: 36 },
  label: { fontSize: 14, fontWeight: '700', marginBottom: 6, color: cores.texto },
  input: {
    height: 52, borderWidth: 1.5, borderColor: cores.borda, borderRadius: 12,
    paddingHorizontal: 16, fontSize: 16, backgroundColor: cores.cartao, marginBottom: 16,
  },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10, marginBottom: 16 },
  botao: { height: 54, borderRadius: 14, backgroundColor: cores.primaria, alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
  servidor: { marginTop: 28, flexDirection: 'row', justifyContent: 'center', gap: 8, padding: 12 },
  servidorTexto: { fontSize: 13, color: cores.textoSecundario },
  servidorLink: { fontSize: 13, fontWeight: '700', color: cores.primaria, textDecorationLine: 'underline' },
});
