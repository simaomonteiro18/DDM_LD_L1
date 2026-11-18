import { useLayoutEffect, useState } from 'react';
import {
  ScrollView, View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet,
} from 'react-native';
import { criarOrcamento, atualizarOrcamento } from '../api/orcamentos';
import { mensagemDeErro } from '../api/client';
import { MESES, formatarDataHora, nomeUtilizador } from '../utils/formatar';
import { cores } from '../tema';

export default function OrcamentoFormScreen({ navigation, route }) {
  // Se vier um orçamento nos parâmetros, estamos a editar; senão, a criar
  const existente = route.params?.orcamento;
  const hoje = new Date();

  const [ano, setAno] = useState(String(existente?.ano ?? hoje.getFullYear()));
  const [mes, setMes] = useState(existente?.mes ?? hoje.getMonth() + 1);
  const [valor, setValor] = useState(existente ? String(existente.valor).replace('.', ',') : '');
  const [erro, setErro] = useState('');
  const [aGuardar, setAGuardar] = useState(false);

  useLayoutEffect(() => {
    navigation.setOptions({ title: existente ? 'Editar orçamento' : 'Novo orçamento' });
  }, [navigation, existente]);

  async function guardar() {
    const anoNumero = Number(ano);
    const valorNumero = Number(valor.trim().replace(',', '.'));

    if (!Number.isInteger(anoNumero) || anoNumero < 2000 || anoNumero > 2100) {
      setErro('Indica um ano válido (ex.: 2026).');
      return;
    }
    if (!valor.trim() || Number.isNaN(valorNumero) || valorNumero <= 0) {
      setErro('Indica um valor maior que zero (ex.: 300,00).');
      return;
    }

    const dados = { ano: anoNumero, mes, valor: Math.round(valorNumero * 100) / 100 };
    setErro('');
    setAGuardar(true);
    try {
      if (existente) {
        await atualizarOrcamento(existente.id, dados);
      } else {
        await criarOrcamento(dados);
      }
      navigation.goBack();
    } catch (e) {
      // Ex.: o backend recusa um segundo orçamento para o mesmo mês
      setErro(mensagemDeErro(e, 'Não foi possível guardar o orçamento.'));
    } finally {
      setAGuardar(false);
    }
  }

  const criadoPor = nomeUtilizador(existente?.criadoPor);
  const alteradoPor = nomeUtilizador(existente?.alteradoPor);

  return (
    <ScrollView style={s.ecra} contentContainerStyle={{ padding: 20, gap: 18 }} keyboardShouldPersistTaps="handled">
      <View>
        <Text style={s.label}>Ano</Text>
        <TextInput
          style={s.input}
          value={ano}
          onChangeText={setAno}
          keyboardType="number-pad"
          maxLength={4}
        />
      </View>

      <View>
        <Text style={s.label}>Mês</Text>
        <View style={s.grelhaMeses}>
          {MESES.map((nome, indice) => {
            const numero = indice + 1;
            const selecionado = numero === mes;
            return (
              <Pressable
                key={nome}
                style={[s.mes, selecionado && s.mesSelecionado]}
                onPress={() => setMes(numero)}
                accessibilityState={{ selected: selecionado }}
              >
                <Text style={[s.mesTexto, selecionado && s.mesTextoSelecionado]}>{nome.slice(0, 3)}</Text>
              </Pressable>
            );
          })}
        </View>
      </View>

      <View>
        <Text style={s.label}>Valor máximo a gastar (€)</Text>
        <TextInput
          style={s.input}
          value={valor}
          onChangeText={setValor}
          keyboardType="decimal-pad"
          placeholder="300,00"
        />
      </View>

      {existente && (criadoPor || existente.dataCriacao) ? (
        <View style={s.info}>
          <Text style={s.infoTexto}>
            Criado por {criadoPor ?? '—'} {formatarDataHora(existente.dataCriacao)}
          </Text>
          {alteradoPor || existente.dataAlterado ? (
            <Text style={s.infoTexto}>
              Alterado por {alteradoPor ?? '—'} {formatarDataHora(existente.dataAlterado)}
            </Text>
          ) : null}
        </View>
      ) : null}

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
  grelhaMeses: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  mes: {
    width: '23%', height: 46, borderRadius: 10, borderWidth: 1.5, borderColor: cores.borda,
    backgroundColor: cores.cartao, alignItems: 'center', justifyContent: 'center',
  },
  mesSelecionado: { backgroundColor: cores.texto, borderColor: cores.texto },
  mesTexto: { fontSize: 14, fontWeight: '700', color: cores.texto },
  mesTextoSelecionado: { color: '#fff' },
  info: { backgroundColor: '#ECEDEA', borderRadius: 12, padding: 12, gap: 4 },
  infoTexto: { fontSize: 13, color: '#4A524D' },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave, padding: 12, borderRadius: 10 },
  botao: { height: 54, borderRadius: 14, backgroundColor: cores.primaria, alignItems: 'center', justifyContent: 'center' },
  botaoTexto: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
