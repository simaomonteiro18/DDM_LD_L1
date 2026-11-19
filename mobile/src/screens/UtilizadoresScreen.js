import { useCallback, useState } from 'react';
import {
  View, Text, FlatList, Pressable, ActivityIndicator, Alert, RefreshControl, StyleSheet,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { listarUtilizadores, apagarUtilizador } from '../api/utilizadores';
import { mensagemDeErro } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { cores } from '../tema';

export default function UtilizadoresScreen({ navigation }) {
  const { user } = useAuth();
  const [utilizadores, setUtilizadores] = useState([]);
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);
  const [erro, setErro] = useState('');

  const carregar = useCallback(async () => {
    setErro('');
    try {
      const dados = await listarUtilizadores();
      dados.sort((a, b) => a.nome.localeCompare(b.nome, 'pt'));
      setUtilizadores(dados);
    } catch (e) {
      setErro(mensagemDeErro(e, 'Não foi possível carregar os utilizadores.'));
    } finally {
      setACarregar(false);
      setAAtualizar(false);
    }
  }, []);

  // Recarrega sempre que se volta a este ecra (ex.: depois de criar ou editar)
  useFocusEffect(useCallback(() => { carregar(); }, [carregar]));

  function confirmarApagar(utilizador) {
    Alert.alert(
      'Apagar utilizador',
      `Tens a certeza que queres apagar o utilizador "${utilizador.nome}"?`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Apagar',
          style: 'destructive',
          onPress: async () => {
            try {
              await apagarUtilizador(utilizador.id);
              setUtilizadores((lista) => lista.filter((u) => u.id !== utilizador.id));
            } catch (e) {
              // Ex.: o backend recusa apagar um utilizador que criou compras ou orcamentos
              Alert.alert('Erro', mensagemDeErro(e, 'Não foi possível apagar o utilizador.'));
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
      <Pressable style={s.botaoNovo} onPress={() => navigation.navigate('UtilizadorForm')}>
        <Text style={s.botaoNovoTexto}>+ Novo utilizador</Text>
      </Pressable>

      {erro ? <Text style={s.erro}>{erro}</Text> : null}

      <FlatList
        data={utilizadores}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ gap: 10, paddingBottom: 24 }}
        refreshControl={
          <RefreshControl refreshing={aAtualizar} onRefresh={() => { setAAtualizar(true); carregar(); }} />
        }
        ListEmptyComponent={
          erro ? null : <Text style={s.vazio}>Ainda não há utilizadores.</Text>
        }
        renderItem={({ item }) => {
          // O backend nao deixa apagar o utilizador autenticado, por isso nao se mostra o botao
          const proprio = item.id === user?.id;
          return (
            <View style={s.cartao}>
              <Pressable
                style={s.cartaoInfo}
                onPress={() => navigation.navigate('UtilizadorForm', { utilizador: item })}
              >
                <Text style={s.nome}>{item.nome}</Text>
                <Text style={s.username}>@{item.username}{proprio ? ' · és tu' : ''}</Text>
              </Pressable>
              {proprio ? null : (
                <Pressable
                  style={s.botaoApagar}
                  onPress={() => confirmarApagar(item)}
                  accessibilityLabel={`Apagar o utilizador ${item.nome}`}
                >
                  <Text style={s.botaoApagarTexto}>Apagar</Text>
                </Pressable>
              )}
            </View>
          );
        }}
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
  cartaoInfo: { flex: 1, paddingVertical: 14, paddingHorizontal: 16, gap: 2 },
  nome: { fontSize: 16, fontWeight: '700', color: cores.texto },
  username: { fontSize: 14, color: cores.textoSecundario },
  botaoApagar: { minHeight: 44, paddingHorizontal: 12, justifyContent: 'center' },
  botaoApagarTexto: { color: cores.erro, fontWeight: '700', fontSize: 14 },
});
