import { useLayoutEffect, useState } from 'react';
import {
  ScrollView, View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet,
} from 'react-native';
import { criarUtilizador, atualizarUtilizador } from '../api/utilizadores';
import { mensagemDeErro } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { cores } from '../tema';

export default function UtilizadorFormScreen({ navigation, route }) {
  // Se vier um utilizador nos parametros, estamos a editar; senao, a criar
  const existente = route.params?.utilizador;
  const { user, atualizarUser } = useAuth();

  const [nome, setNome] = useState(existente?.nome ?? '');
  const [username, setUsername] = useState(existente?.username ?? '');
  const [password, setPassword] = useState('');
  const [erro, setErro] = useState('');
  const [aGuardar, setAGuardar] = useState(false);

  useLayoutEffect(() => {
    navigation.setOptions({ title: existente ? 'Editar utilizador' : 'Novo utilizador' });
  }, [navigation, existente]);

  async function guardar() {
    const nomeLimpo = nome.trim();
    const usernameLimpo = username.trim();
    if (!nomeLimpo) {
      setErro('Indica o nome do utilizador (ex.: Ana Silva).');
      return;
    }
    if (!usernameLimpo) {
      setErro('Indica o username (ex.: ana.silva).');
      return;
    }
    if (/\s/.test(usernameLimpo)) {
      setErro('O username não pode conter espaços.');
      return;
    }
    // Na edicao a palavra-passe e opcional; as restantes regras sao validadas pelo backend
    if (!existente && !password) {
      setErro('Indica a palavra-passe.');
      return;
    }
    if (password && password.length < 6) {
      setErro('A palavra-passe tem de ter pelo menos 6 caracteres.');
      return;
    }

    setErro('');
    setAGuardar(true);
    try {
      const dados = { nome: nomeLimpo, username: usernameLimpo, password };
      if (existente) {
        const atualizado = await atualizarUtilizador(existente.id, dados);
        // Se alterou o proprio utilizador, o separador Mais passa a mostrar os dados novos
        if (atualizado.id === user?.id) atualizarUser(atualizado);
      } else {
        await criarUtilizador(dados);
      }
      navigation.goBack();
    } catch (e) {
      // Ex.: o backend recusa um username que ja existe
      setErro(mensagemDeErro(e, 'Não foi possível guardar o utilizador.'));
    } finally {
      setAGuardar(false);
    }
  }

  return (
    <ScrollView style={s.ecra} contentContainerStyle={{ padding: 20, gap: 18 }} keyboardShouldPersistTaps="handled">
      <View>
        <Text style={s.label}>Nome</Text>
        <TextInput
          style={s.input}
          value={nome}
          onChangeText={setNome}
          placeholder="Ana Silva"
          maxLength={100}
        />
      </View>

      <View>
        <Text style={s.label}>Username</Text>
        <TextInput
          style={s.input}
          value={username}
          onChangeText={setUsername}
          placeholder="ana.silva"
          maxLength={50}
          autoCapitalize="none"
          autoCorrect={false}
        />
      </View>

      <View>
        <Text style={s.label}>Palavra-passe</Text>
        <TextInput
          style={s.input}
          value={password}
          onChangeText={setPassword}
          maxLength={72}
          secureTextEntry
        />
        {existente ? (
          <Text style={s.ajuda}>Deixa em branco para manter a palavra-passe atual.</Text>
        ) : null}
      </View>

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <Pressable style={[s.botao, aGuardar && { opacity: 0.6 }]} onPress={guardar} disabled={aGuardar}>
        {aGuardar ? <ActivityIndicator color="#fff" /> : <Text style={s.botaoTexto}>Guardar</Text>}
      </Pressable>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, backgroundColor: cores.fundo },
  label: { fontSize: 14, fontWeight: '700', marginBottom: 8, color: cores.texto },
  input: {
    height: 52, borderWidth: 1.5, borderColor: cores.borda, borderRadius: 12,
    paddingHorizontal: 16, fontSize: 16, backgroundColor: cores.cartao,
  },
  ajuda: { fontSize: 13, color: cores.textoSecundario, marginTop: 6 },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10 },
  botao: { height: 54, borderRadius: 14, backgroundColor: cores.primaria, alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
