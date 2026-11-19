import { useCallback, useState } from 'react';
import { View, Text, Pressable, ScrollView, ActivityIndicator, RefreshControl, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import api, { mensagemDeErro } from '../api/client';
import { nomeDoMes, formatarEuro, formatarDiaMesLisboa } from '../utils/formatar';
import { cores } from '../tema';

// Ecra Inicio: orcamento do mes e compras abertas
export default function HomeScreen({ navigation }) {
  const [orcamento, setOrcamento] = useState(null);
  const [semOrcamento, setSemOrcamento] = useState(false);
  const [erroOrcamento, setErroOrcamento] = useState('');
  const [compras, setCompras] = useState([]);
  const [erroCompras, setErroCompras] = useState('');
  const [aCarregar, setACarregar] = useState(true);
  const [aAtualizar, setAAtualizar] = useState(false);

  const carregar = useCallback(async () => {
    const [resOrcamento, resCompras] = await Promise.allSettled([
      api.get('/orcamentos/disponivel'),
      api.get('/compras', { params: { fechada: false } }),
    ]);

    if (resOrcamento.status === 'fulfilled') {
      setOrcamento(resOrcamento.value.data);
      setSemOrcamento(false);
      setErroOrcamento('');
    } else if (resOrcamento.reason?.response?.status === 404) {
      setOrcamento(null);
      setSemOrcamento(true);
      setErroOrcamento('');
    } else {
      setErroOrcamento(mensagemDeErro(resOrcamento.reason, 'Não foi possível carregar o orçamento.'));
    }

    if (resCompras.status === 'fulfilled') {
      setCompras(resCompras.value.data);
      setErroCompras('');
    } else {
      setErroCompras(mensagemDeErro(resCompras.reason, 'Não foi possível carregar as compras.'));
    }

    setACarregar(false);
    setAAtualizar(false);
  }, []);

  // Volta a carregar sempre que o ecra fica em foco (ex.: ao regressar de outro ecra)
  useFocusEffect(useCallback(() => { carregar(); }, [carregar]));

  function atualizar() {
    setAAtualizar(true);
    carregar();
  }

  function tentarOutraVez() {
    setACarregar(true);
    carregar();
  }

  if (aCarregar) {
    return (
      <View style={s.centro}>
        <ActivityIndicator size="large" color={cores.primaria} />
      </View>
    );
  }

  return (
    <ScrollView
      style={s.fundo}
      contentContainerStyle={s.conteudo}
      refreshControl={<RefreshControl refreshing={aAtualizar} onRefresh={atualizar} colors={[cores.primaria]} />}
    >
      <CartaoOrcamento
        orcamento={orcamento}
        semOrcamento={semOrcamento}
        erro={erroOrcamento}
        onDefinir={() => navigation.navigate('Orcamentos')}
        onTentarOutraVez={tentarOutraVez}
      />

      <View style={s.cabecalhoSeccao}>
        <Text style={s.seccao}>Compras abertas</Text>
        <Pressable
          onPress={() => navigation.navigate('Compras')}
          hitSlop={12}
          accessibilityRole="link"
        >
          <Text style={s.verTodas}>Ver todas</Text>
        </Pressable>
      </View>

      {erroCompras ? (
        <CaixaErro mensagem={erroCompras} onTentarOutraVez={tentarOutraVez} />
      ) : compras.length === 0 ? (
        <Text style={s.vazio}>Não há compras abertas.</Text>
      ) : (
        compras.map((compra) => (
          <CartaoCompra
            key={compra.id}
            compra={compra}
            onEditar={() => navigation.navigate('EditarCompra', { idCompra: compra.id, detalhe: compra.nome })}
            onIniciar={() => navigation.navigate('ModoCompra', { idCompra: compra.id, detalhe: compra.nome })}
          />
        ))
      )}
    </ScrollView>
  );
}

function CartaoOrcamento({ orcamento, semOrcamento, erro, onDefinir, onTentarOutraVez }) {
  if (erro) {
    return <CaixaErro mensagem={erro} onTentarOutraVez={onTentarOutraVez} />;
  }
  if (semOrcamento || !orcamento) {
    return (
      <View style={s.cartao}>
        <Text style={s.semOrcamento}>Sem orçamento definido para este mês</Text>
        <Pressable
          style={({ pressed }) => [s.botaoPrimario, pressed && s.premido]}
          onPress={onDefinir}
          accessibilityRole="button"
        >
          <Text style={s.botaoPrimarioTexto}>Definir orçamento</Text>
        </Pressable>
      </View>
    );
  }
  const { mes, orcamento: valor, gasto, disponivel, percentagemRestante, ultrapassado } = orcamento;
  return (
    <View style={s.cartao}>
      <View style={s.linhaTitulo}>
        <Text style={s.tituloOrcamento}>Orçamento de {nomeDoMes(mes).toLowerCase()}</Text>
        {ultrapassado ? (
          <Text style={[s.selo, s.seloErro]}>Ultrapassado</Text>
        ) : percentagemRestante != null ? (
          <Text style={s.selo}>Restam {percentagemRestante}%</Text>
        ) : null}
      </View>
      <Text style={s.rotulo}>Disponível</Text>
      <Text style={[s.disponivel, ultrapassado && s.textoErro]}>{formatarEuro(disponivel)}</Text>
      <Text style={s.gastos}>Gastos {formatarEuro(gasto)} de {formatarEuro(valor)}</Text>
    </View>
  );
}

function CartaoCompra({ compra, onEditar, onIniciar }) {
  const total = compra.totalItensPrevistos ?? 0;
  const itens = total === 1 ? '1 item previsto' : `${total} itens previstos`;
  return (
    <View style={s.cartao}>
      <Text style={s.nomeCompra}>{compra.nome}</Text>
      <Text style={s.detalheCompra}>
        {itens}, criado por {compra.criadoPor?.nome ?? compra.criadoPor?.username} · {formatarDiaMesLisboa(compra.dataCriacao)}
      </Text>
      <View style={s.botoes}>
        <Pressable
          style={({ pressed }) => [s.botaoSecundario, pressed && s.premido]}
          onPress={onEditar}
          accessibilityRole="button"
          accessibilityLabel={`Editar a compra ${compra.nome}`}
        >
          <Text style={s.botaoSecundarioTexto}>Editar</Text>
        </Pressable>
        <Pressable
          style={({ pressed }) => [s.botaoPrimario, s.botaoFlex, pressed && s.premido]}
          onPress={onIniciar}
          accessibilityRole="button"
          accessibilityLabel={`Iniciar a compra ${compra.nome}`}
        >
          <Text style={s.botaoPrimarioTexto}>Iniciar compra</Text>
        </Pressable>
      </View>
    </View>
  );
}

function CaixaErro({ mensagem, onTentarOutraVez }) {
  return (
    <View style={s.caixaErro}>
      <Text style={s.erro}>{mensagem}</Text>
      <Pressable
        style={({ pressed }) => [s.botaoSecundario, pressed && s.premido]}
        onPress={onTentarOutraVez}
        accessibilityRole="button"
      >
        <Text style={s.botaoSecundarioTexto}>Tentar outra vez</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  centro: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: cores.fundo },
  fundo: { flex: 1, backgroundColor: cores.fundo },
  conteudo: { padding: 16, paddingBottom: 32 },
  cartao: {
    backgroundColor: cores.cartao, borderWidth: 1, borderColor: cores.bordaSuave, borderRadius: 14,
    padding: 16, marginBottom: 12, gap: 6,
  },
  linhaTitulo: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' },
  tituloOrcamento: { flexShrink: 1, fontSize: 16, fontWeight: '700', color: cores.texto },
  selo: {
    fontSize: 13, fontWeight: '700', color: cores.primaria, backgroundColor: cores.primariaSuave,
    paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999, overflow: 'hidden',
  },
  seloErro: { color: cores.erro, backgroundColor: cores.erroSuave },
  rotulo: { fontSize: 13, color: cores.textoSecundario, marginTop: 4 },
  disponivel: { fontSize: 32, fontWeight: '800', color: cores.texto },
  textoErro: { color: cores.erro },
  gastos: { fontSize: 14, color: cores.textoSecundario },
  semOrcamento: { fontSize: 16, fontWeight: '600', color: cores.texto, marginBottom: 8 },
  cabecalhoSeccao: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 16, marginBottom: 10,
  },
  seccao: { fontSize: 18, fontWeight: '800', color: cores.texto },
  verTodas: { fontSize: 15, fontWeight: '700', color: cores.primaria },
  vazio: { fontSize: 15, color: cores.textoSecundario, paddingVertical: 16 },
  nomeCompra: { fontSize: 16, fontWeight: '700', color: cores.texto },
  detalheCompra: { fontSize: 14, color: cores.textoSecundario },
  botoes: { flexDirection: 'row', gap: 10, marginTop: 8 },
  botaoPrimario: {
    minHeight: 48, borderRadius: 12, backgroundColor: cores.primaria,
    alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16,
  },
  botaoFlex: { flex: 1 },
  botaoPrimarioTexto: { color: '#fff', fontSize: 15, fontWeight: '700' },
  botaoSecundario: {
    minHeight: 48, borderRadius: 12, borderWidth: 1.5, borderColor: cores.borda, backgroundColor: cores.cartao,
    alignItems: 'center', justifyContent: 'center', paddingHorizontal: 16,
  },
  botaoSecundarioTexto: { color: cores.texto, fontSize: 15, fontWeight: '700' },
  premido: { opacity: 0.6 },
  caixaErro: { backgroundColor: cores.erroSuave, borderRadius: 12, padding: 14, gap: 12, marginBottom: 12 },
  erro: { color: cores.erro, fontSize: 15 },
});
