import { useCallback, useState } from 'react';
import { View, Text, Pressable, ScrollView, ActivityIndicator, RefreshControl, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import api, { mensagemDeErro } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ENTRADAS_MENU } from '../navigation/rotas';
import { cores } from '../tema';

export default function HomeScreen({ navigation }) {
  const { user, sair } = useAuth();
  const [compras, setCompras] = useState([]);
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);
  const [erro, setErro] = useState('');

  const carregarCompras = useCallback(async () => {
    try {
      const res = await api.get('/compras', { params: { fechada: false } });
      setCompras(res.data);
      setErro('');
    } catch (e) {
      setErro(mensagemDeErro(e, 'Não foi possível carregar as compras.'));
    } finally {
      setACarregar(false);
      setAAtualizar(false);
    }
  }, []);

  // Volta a carregar sempre que o ecra fica em foco (ex.: ao regressar de outro ecra)
  useFocusEffect(useCallback(() => { carregarCompras(); }, [carregarCompras]));

  function tentarOutraVez() {
    setACarregar(true);
    carregarCompras();
  }

  function atualizar() {
    setAAtualizar(true);
    carregarCompras();
  }

  function abrirCompra(compra) {
    navigation.navigate('ModoCompra', { idCompra: compra.id, detalhe: compra.nome });
  }

  return (
    <ScrollView
      style={s.fundo}
      contentContainerStyle={s.conteudo}
      refreshControl={<RefreshControl refreshing={aAtualizar} onRefresh={atualizar} colors={[cores.primaria]} />}
    >
      <Text style={s.saudacao}>Olá, {user?.nome ?? user?.username}</Text>
      <Text style={s.subtitulo}>O que queres fazer?</Text>

      <View style={s.menu}>
        {ENTRADAS_MENU.map((entrada) => (
          <Pressable
            key={entrada.nome}
            style={({ pressed }) => [s.botaoMenu, pressed && s.premido]}
            onPress={() => navigation.navigate(entrada.nome)}
            accessibilityRole="button"
            accessibilityLabel={entrada.titulo}
          >
            <Text style={s.botaoMenuTitulo}>{entrada.titulo}</Text>
            <Text style={s.botaoMenuDescricao}>{entrada.descricao}</Text>
          </Pressable>
        ))}
      </View>

      <Text style={s.seccao}>Compras em aberto</Text>
      {aCarregar ? (
        <ActivityIndicator style={s.carregar} size="large" color={cores.primaria} />
      ) : erro ? (
        <View style={s.caixaErro}>
          <Text style={s.erro}>{erro}</Text>
          <Pressable
            style={({ pressed }) => [s.botaoSecundario, pressed && s.premido]}
            onPress={tentarOutraVez}
            accessibilityRole="button"
          >
            <Text style={s.botaoSecundarioTexto}>Tentar outra vez</Text>
          </Pressable>
        </View>
      ) : compras.length === 0 ? (
        <Text style={s.vazio}>Não há compras em aberto.</Text>
      ) : (
        compras.map((compra) => (
          <Pressable
            key={compra.id}
            style={({ pressed }) => [s.compra, pressed && s.premido]}
            onPress={() => abrirCompra(compra)}
            accessibilityRole="button"
            accessibilityLabel={`Abrir a compra ${compra.nome}`}
          >
            <Text style={s.compraNome}>{compra.nome}</Text>
            <Text style={s.compraSeta}>›</Text>
          </Pressable>
        ))
      )}

      <Pressable
        style={({ pressed }) => [s.botaoSecundario, s.sair, pressed && s.premido]}
        onPress={sair}
        accessibilityRole="button"
      >
        <Text style={s.botaoSecundarioTexto}>Terminar sessão</Text>
      </Pressable>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  fundo: { flex: 1, backgroundColor: cores.fundo },
  conteudo: { padding: 20, paddingBottom: 32 },
  saudacao: { fontSize: 26, fontWeight: '800', color: cores.texto },
  subtitulo: { fontSize: 15, color: cores.textoSecundario, marginTop: 4, marginBottom: 20 },
  menu: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  botaoMenu: {
    flexGrow: 1, flexBasis: '45%', minHeight: 96, padding: 16, borderRadius: 14,
    backgroundColor: cores.cartao, borderWidth: 1.5, borderColor: cores.bordaSuave, justifyContent: 'center',
  },
  botaoMenuTitulo: { fontSize: 17, fontWeight: '700', color: cores.primaria },
  botaoMenuDescricao: { fontSize: 13, color: cores.textoSecundario, marginTop: 4 },
  premido: { opacity: 0.6 },
  seccao: { fontSize: 18, fontWeight: '800', color: cores.texto, marginTop: 28, marginBottom: 12 },
  carregar: { marginVertical: 24 },
  vazio: { fontSize: 15, color: cores.textoSecundario, paddingVertical: 16 },
  caixaErro: { backgroundColor: cores.erroSuave, borderRadius: 12, padding: 14, gap: 12 },
  erro: { color: cores.erro, fontSize: 15 },
  compra: {
    flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingHorizontal: 16, paddingVertical: 14,
    marginBottom: 10, borderRadius: 12, backgroundColor: cores.cartao, borderWidth: 1.5, borderColor: cores.bordaSuave,
  },
  compraNome: { flex: 1, fontSize: 16, fontWeight: '600', color: cores.texto },
  compraSeta: { fontSize: 24, color: cores.textoSecundario, marginLeft: 12 },
  botaoSecundario: {
    minHeight: 52, borderRadius: 14, borderWidth: 1.5, borderColor: cores.borda, backgroundColor: cores.cartao,
    alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16,
  },
  botaoSecundarioTexto: { color: cores.texto, fontSize: 15, fontWeight: '700' },
  sair: { marginTop: 28 },
});
