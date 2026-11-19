import { useCallback, useState } from 'react';
import {
  View, Text, FlatList, Pressable, ActivityIndicator, Alert, RefreshControl, StyleSheet,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { listarTiposArtigo, apagarTipoArtigo } from '../api/tiposArtigo';
import { mensagemDeErro } from '../api/client';
import { cores } from '../tema';

export default function TiposArtigoScreen({ navigation }) {
  const [tipos, setTipos] = useState([]);
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setErro('');
    try {
      const dados = await listarTiposArtigo();
      dados.sort((a, b) => a.nome.localeCompare(b.nome, 'pt'));
      setTipos(dados);
    } catch (e) {
      setErro(mensagemDeErro(e, 'Não foi possível carregar os tipos de artigo.'));
    } finally {
      setACarregar(false);
      setAAtualizar(false);
    }
  }, []);

  // Recarrega sempre que se volta a este ecrã (ex.: depois de criar ou editar)
  useFocusEffect(useCallback(() => { carregar(); }, [carregar]));

  function confirmarApagar(tipo) {
    Alert.alert(
      'Apagar tipo de artigo',
      `Tens a certeza que queres apagar o tipo "${tipo.nome}"?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Apagar',
          style: 'destructive',
          onPress: async () => {
            try {
              await apagarTipoArtigo(tipo.id);
              setTipos((lista) => lista.filter((t) => t.id !== tipo.id));
            } catch (e) {
              // Ex.: o backend recusa apagar um tipo que ainda tem artigos
              Alert.alert('Erro', mensagemDeErro(e, 'Não foi possível apagar o tipo de artigo.'));
            }
          },
        },
      ],
    );
  }

  if (aCarregar) {
    return (
      <View style={s.centro}>
        <ActivityIndicator size="large" color={cores.primaria} />
      </View>
    );
  }

  return (
    <View style={s.ecra}>
      <Pressable style={s.botaoNovo} onPress={() => navigation.navigate('TipoArtigoForm')}>
        <Text style={s.botaoNovoTexto}>+ Novo tipo de artigo</Text>
      </Pressable>

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <FlatList
        data={tipos}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ gap: 10, paddingBottom: 24 }}
        refreshControl={
          <RefreshControl refreshing={aAtualizar} onRefresh={() => { setAAtualizar(true); carregar(); }} />
        }
        ListEmptyComponent={
          erro ? null : <Text style={s.vazio}>Ainda não há tipos de artigo. Cria o primeiro.</Text>
        }
        renderItem={({ item }) => (
          <View style={s.cartao}>
            <Pressable
              style={s.cartaoInfo}
              onPress={() => navigation.navigate('TipoArtigoForm', { tipo: item })}
            >
              <Text style={s.nome}>{item.nome}</Text>
            </Pressable>
            <Pressable
              style={s.botaoApagar}
              onPress={() => confirmarApagar(item)}
              accessibilityLabel={`Apagar o tipo de artigo ${item.nome}`}
            >
              <Text style={s.botaoApagarTexto}>Apagar</Text>
            </Pressable>
          </View>
        )}
      />
    </View>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, padding: 16, backgroundColor: cores.fundo },
  centro: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: cores.fundo },
  botaoNovo: {
    height: 52, borderRadius: 14, backgroundColor: cores.primaria,
    alignItems: 'center', justifyContent: 'center', marginBottom: 16,
  },
  botaoNovoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10, marginBottom: 16 },
  vazio: { textAlign: 'center', color: cores.textoSecundario, marginTop: 40, fontSize: 15 },
  cartao: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: cores.cartao,
    borderWidth: 1, borderColor: cores.bordaSuave, borderRadius: 14, paddingRight: 8,
  },
  cartaoInfo: { flex: 1, paddingVertical: 14, paddingHorizontal: 16 },
  nome: { fontSize: 16, fontWeight: '700', color: cores.texto },
  botaoApagar: { minHeight: 44, paddingHorizontal: 12, justifyContent: 'center' },
  botaoApagarTexto: { color: cores.erro, fontWeight: '700', fontSize: 14 },
});
