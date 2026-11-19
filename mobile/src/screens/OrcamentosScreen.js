import { useCallback, useState } from 'react';
import {
  View, Text, FlatList, Pressable, ActivityIndicator, Alert, RefreshControl, StyleSheet,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { listarOrcamentos, apagarOrcamento } from '../api/orcamentos';
import { mensagemDeErro } from '../api/client';
import { nomeDoMes, formatarEuro } from '../utils/formatar';
import { cores } from '../tema';

export default function OrcamentosScreen({ navigation }) {
  const [orcamentos, setOrcamentos] = useState([]);
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setErro('');
    try {
      const dados = await listarOrcamentos();
      // Mais recentes primeiro
      dados.sort((a, b) => b.ano - a.ano || b.mes - a.mes);
      setOrcamentos(dados);
    } catch (e) {
      setErro(mensagemDeErro(e, 'Não foi possível carregar os orçamentos.'));
    } finally {
      setACarregar(false);
      setAAtualizar(false);
    }
  }, []);

  // Recarrega sempre que se volta a este ecrã (ex.: depois de criar ou editar)
  useFocusEffect(useCallback(() => { carregar(); }, [carregar]));

  function confirmarApagar(orcamento) {
    Alert.alert(
      'Apagar orçamento',
      `Tens a certeza que queres apagar o orçamento de ${nomeDoMes(orcamento.mes)} ${orcamento.ano}?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Apagar',
          style: 'destructive',
          onPress: async () => {
            try {
              await apagarOrcamento(orcamento.id);
              setOrcamentos((lista) => lista.filter((o) => o.id !== orcamento.id));
            } catch (e) {
              Alert.alert('Erro', mensagemDeErro(e, 'Não foi possível apagar o orçamento.'));
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
      <Pressable style={s.botaoNovo} onPress={() => navigation.navigate('OrcamentoForm')}>
        <Text style={s.botaoNovoTexto}>+ Novo orçamento</Text>
      </Pressable>

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <FlatList
        data={orcamentos}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ gap: 10, paddingBottom: 24 }}
        refreshControl={
          <RefreshControl refreshing={aAtualizar} onRefresh={() => { setAAtualizar(true); carregar(); }} />
        }
        ListEmptyComponent={
          erro ? null : <Text style={s.vazio}>Ainda não há orçamentos. Cria o primeiro.</Text>
        }
        renderItem={({ item }) => (
          <View style={s.cartao}>
            <Pressable
              style={s.cartaoInfo}
              onPress={() => navigation.navigate('OrcamentoForm', { orcamento: item })}
            >
              <Text style={s.mes}>{nomeDoMes(item.mes)} {item.ano}</Text>
              <Text style={s.valor}>{formatarEuro(item.valor)}</Text>
            </Pressable>
            <Pressable
              style={s.botaoApagar}
              onPress={() => confirmarApagar(item)}
              accessibilityLabel={`Apagar orçamento de ${nomeDoMes(item.mes)} ${item.ano}`}
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
  cartaoInfo: { flex: 1, paddingVertical: 14, paddingHorizontal: 16, gap: 4 },
  mes: { fontSize: 16, fontWeight: '700', color: cores.texto },
  valor: { fontSize: 15, color: cores.textoSecundario },
  botaoApagar: { minHeight: 44, paddingHorizontal: 12, justifyContent: 'center' },
  botaoApagarTexto: { color: cores.erro, fontWeight: '700', fontSize: 14 },
});
