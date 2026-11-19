import { useLayoutEffect, useState } from 'react';
import {
  ScrollView, View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet,
} from 'react-native';
import { criarTipoArtigo, atualizarTipoArtigo } from '../api/tiposArtigo';
import { mensagemDeErro } from '../api/client';
import { cores } from '../tema';

export default function TipoArtigoFormScreen({ navigation, route }) {
  // Se vier um tipo nos parâmetros, estamos a editar; senão, a criar
  const existente = route.params?.tipo;

  const [nome, setNome] = useState(existente?.nome ?? '');
  const [erro, setErro] = useState('');
  const [aGuardar, setAGuardar] = useState(false);

  useLayoutEffect(() => {
    navigation.setOptions({ title: existente ? 'Editar tipo de artigo' : 'Novo tipo de artigo' });
  }, [navigation, existente]);

  async function guardar() {
    const nomeLimpo = nome.trim();
    if (!nomeLimpo) {
      setErro('Indica o nome do tipo de artigo (ex.: Laticínios).');
      return;
    }

    setErro('');
    setAGuardar(true);
    try {
      if (existente) {
        await atualizarTipoArtigo(existente.id, { nome: nomeLimpo });
      } else {
        await criarTipoArtigo({ nome: nomeLimpo });
      }
      navigation.goBack();
    } catch (e) {
      // Ex.: o backend recusa um nome que já existe
      setErro(mensagemDeErro(e, 'Não foi possível guardar o tipo de artigo.'));
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
          placeholder="Laticínios"
          maxLength={100}
        />
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
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10 },
  botao: { height: 54, borderRadius: 14, backgroundColor: cores.primaria, alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
