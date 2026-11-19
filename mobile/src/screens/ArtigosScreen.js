import { useCallback, useState } from 'react';
import {
  View, Text, FlatList, ScrollView, Pressable, ActivityIndicator, Alert, RefreshControl, StyleSheet,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { listarArtigos, apagarArtigo, idDoTipo, nomeDoTipo } from '../api/artigos';
import { listarTiposArtigo } from '../api/tiposArtigo';
import { mensagemDeErro } from '../api/client';
import { cores } from '../tema';

export default function ArtigosScreen({ navigation }) {
  const [artigos, setArtigos] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [filtro, setFiltro] = useState(null); // id do tipo escolhido; null = Todos
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setErro('');
    try {
      const [listaArtigos, listaTipos] = await Promise.all([listarArtigos(), listarTiposArtigo()]);
      listaArtigos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt'));
      listaTipos.sort((a, b) => a.nome.localeCompare(b.nome, 'pt'));
      setArtigos(listaArtigos);
      setTipos(listaTipos);
    } catch (e) {
      setErro(mensagemDeErro(e, 'Não foi possível carregar os artigos.'));
    } finally {
      setACarregar(false);
      setAAtualizar(false);
    }
  }, []);

  // Recarrega sempre que se volta a este ecrã (ex.: depois de criar ou editar)
  useFocusEffect(useCallback(() => { carregar(); }, [carregar]));

  const visiveis = filtro === null ? artigos : artigos.filter((a) => idDoTipo(a) === filtro);

  function tipoDe(artigo) {
    const id = idDoTipo(artigo);
    return tipos.find((t) => t.id === id)?.nome ?? nomeDoTipo(artigo);
  }

  function confirmarApagar(artigo) {
    Alert.alert(
      'Apagar artigo',
      `Tens a certeza que queres apagar o artigo "${artigo.nome}"?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Apagar',
          style: 'destructive',
          onPress: async () => {
            try {
              await apagarArtigo(artigo.id);
              setArtigos((lista) => lista.filter((a) => a.id !== artigo.id));
            } catch (e) {
              Alert.alert('Erro', mensagemDeErro(e, 'Não foi possível apagar o artigo.'));
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
      <Pressable
        style={s.botaoNovo}
        onPress={() => navigation.navigate('ArtigoForm', { tipoId: filtro })}
      >
        <Text style={s.botaoNovoTexto}>+ Novo artigo</Text>
      </Pressable>

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <View>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.filtros}>
          <Pressable
            style={[s.chip, filtro === null && s.chipAtivo]}
            onPress={() => setFiltro(null)}
            accessibilityState={{ selected: filtro === null }}
          >
            <Text style={[s.chipTexto, filtro === null && s.chipTextoAtivo]}>Todos</Text>
          </Pressable>
          {tipos.map((tipo) => (
            <Pressable
              key={tipo.id}
              style={[s.chip, filtro === tipo.id && s.chipAtivo]}
              onPress={() => setFiltro(tipo.id)}
              accessibilityState={{ selected: filtro === tipo.id }}
            >
              <Text style={[s.chipTexto, filtro === tipo.id && s.chipTextoAtivo]}>{tipo.nome}</Text>
            </Pressable>
          ))}
        </ScrollView>
      </View>

      <FlatList
        data={visiveis}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ gap: 10, paddingBottom: 24 }}
        refreshControl={
          <RefreshControl refreshing={aAtualizar} onRefresh={() => { setAAtualizar(true); carregar(); }} />
        }
        ListEmptyComponent={
          erro ? null : (
            <Text style={s.vazio}>
              {filtro === null ? 'Ainda não há artigos. Cria o primeiro.' : 'Não há artigos deste tipo.'}
            </Text>
          )
        }
        renderItem={({ item }) => (
          <View style={s.cartao}>
            <Pressable
              style={s.cartaoInfo}
              onPress={() => navigation.navigate('ArtigoForm', { artigo: item })}
            >
              <Text style={s.nome}>{item.nome}</Text>
              {tipoDe(item) ? <Text style={s.tipo}>{tipoDe(item)}</Text> : null}
            </Pressable>
            <Pressable
              style={s.botaoApagar}
              onPress={() => confirmarApagar(item)}
              accessibilityLabel={`Apagar o artigo ${item.nome}`}
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
  filtros: { gap: 8, paddingBottom: 14 },
  chip: {
    height: 36, paddingHorizontal: 16, borderRadius: 18, borderWidth: 1.5, borderColor: cores.borda,
    backgroundColor: cores.cartao, alignItems: 'center', justifyContent: 'center',
  },
  chipAtivo: { backgroundColor: cores.texto, borderColor: cores.texto },
  chipTexto: { fontSize: 14, fontWeight: '700', color: cores.texto },
  chipTextoAtivo: { color: '#fff' },
  vazio: { textAlign: 'center', color: cores.textoSecundario, marginTop: 40, fontSize: 15 },
  cartao: {
    flexDirection: 'row', alignItems: 'center', backgroundColor: cores.cartao,
    borderWidth: 1, borderColor: cores.bordaSuave, borderRadius: 14, paddingRight: 8,
  },
  cartaoInfo: { flex: 1, paddingVertical: 14, paddingHorizontal: 16, gap: 4 },
  nome: { fontSize: 16, fontWeight: '700', color: cores.texto },
  tipo: { fontSize: 14, color: cores.textoSecundario },
  botaoApagar: { minHeight: 44, paddingHorizontal: 12, justifyContent: 'center' },
  botaoApagarTexto: { color: cores.erro, fontWeight: '700', fontSize: 14 },
});
