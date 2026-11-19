import { useEffect, useLayoutEffect, useState } from 'react';
import {
  ScrollView, View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet,
} from 'react-native';
import { criarArtigo, atualizarArtigo, idDoTipo } from '../api/artigos';
import { listarTiposArtigo } from '../api/tiposArtigo';
import { mensagemDeErro } from '../api/client';
import { cores } from '../tema';

export default function ArtigoFormScreen({ navigation, route }) {
  // Se vier um artigo nos parâmetros, estamos a editar; senão, a criar
  const existente = route.params?.artigo;
  const tipoInicial = route.params?.tipoId ?? null; // filtro ativo na lista (só ao criar)

  const [nome, setNome] = useState(existente?.nome ?? '');
  const [tipoId, setTipoId] = useState(existente ? idDoTipo(existente) : tipoInicial);
  const [tipos, setTipos] = useState([]);
  const [aCarregar, setACarregar] = useState(true);
  const [erro, setErro] = useState('');
  const [aGuardar, setAGuardar] = useState(false);

  useLayoutEffect(() => {
    navigation.setOptions({ title: existente ? 'Editar artigo' : 'Novo artigo' });
  }, [navigation, existente]);

  useEffect(() => {
    (async () => {
      try {
        const dados = await listarTiposArtigo();
        dados.sort((a, b) => a.nome.localeCompare(b.nome, 'pt'));
        setTipos(dados);
      } catch (e) {
        setErro(mensagemDeErro(e, 'Não foi possível carregar os tipos de artigo.'));
      } finally {
        setACarregar(false);
      }
    })();
  }, []);

  async function guardar() {
    const nomeLimpo = nome.trim();
    if (!nomeLimpo) {
      setErro('Indica o nome do artigo (ex.: Leite).');
      return;
    }
    if (tipoId === null || tipoId === undefined) {
      setErro('Escolhe o tipo de artigo.');
      return;
    }

    setErro('');
    setAGuardar(true);
    try {
      const dados = { nome: nomeLimpo, tipoId };
      if (existente) {
        await atualizarArtigo(existente.id, dados);
      } else {
        await criarArtigo(dados);
      }
      navigation.goBack();
    } catch (e) {
      // Ex.: o backend recusa um nome que já existe
      setErro(mensagemDeErro(e, 'Não foi possível guardar o artigo.'));
    } finally {
      setAGuardar(false);
    }
  }

  if (aCarregar) {
    return (
      <View style={s.centro}>
        <ActivityIndicator size="large" color={cores.primaria} />
      </View>
    );
  }

  return (
    <ScrollView style={s.ecra} contentContainerStyle={{ padding: 20, gap: 18 }} keyboardShouldPersistTaps="handled">
      <View>
        <Text style={s.label}>Nome</Text>
        <TextInput
          style={s.input}
          value={nome}
          onChangeText={setNome}
          placeholder="Leite"
          maxLength={100}
        />
      </View>

      <View>
        <Text style={s.label}>Tipo de artigo</Text>
        {tipos.length === 0 ? (
          <Text style={s.aviso}>Ainda não há tipos de artigo. Cria primeiro um tipo em Mais › Tipos de artigo.</Text>
        ) : (
          <View style={s.grelhaTipos}>
            {tipos.map((tipo) => {
              const selecionado = tipo.id === tipoId;
              return (
                <Pressable
                  key={tipo.id}
                  style={[s.tipo, selecionado && s.tipoSelecionado]}
                  onPress={() => setTipoId(tipo.id)}
                  accessibilityState={{ selected: selecionado }}
                >
                  <Text style={[s.tipoTexto, selecionado && s.tipoTextoSelecionado]}>{tipo.nome}</Text>
                </Pressable>
              );
            })}
          </View>
        )}
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
  centro: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: cores.fundo },
  label: { fontSize: 14, fontWeight: '700', marginBottom: 8, color: cores.texto },
  input: {
    height: 52, borderWidth: 1.5, borderColor: cores.borda, borderRadius: 12,
    paddingHorizontal: 16, fontSize: 16, backgroundColor: cores.cartao,
  },
  aviso: { fontSize: 14, color: cores.textoSecundario },
  grelhaTipos: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  tipo: {
    minHeight: 44, paddingHorizontal: 16, borderRadius: 10, borderWidth: 1.5, borderColor: cores.borda,
    backgroundColor: cores.cartao, alignItems: 'center', justifyContent: 'center',
  },
  tipoSelecionado: { backgroundColor: cores.texto, borderColor: cores.texto },
  tipoTexto: { fontSize: 14, fontWeight: '700', color: cores.texto },
  tipoTextoSelecionado: { color: '#fff' },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10 },
  botao: { height: 54, borderRadius: 14, backgroundColor: cores.primaria, alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
