import { useState } from 'react';
import { View, Text, TextInput, Pressable, ActivityIndicator, StyleSheet } from 'react-native';
import axios from 'axios';
import { definirServidor, obterServidor } from '../api/client';
import { URL_PADRAO, normalizarUrl, urlValido, guardarUrl, reporUrl } from '../config/servidor';
import { cores } from '../tema';

export default function ServidorScreen({ navigation }) {
  const [url, setUrl] = useState(obterServidor());
  const [mensagem, setMensagem] = useState(null); // { tipo: 'ok' | 'erro', texto }
  const [aTestar, setATestar] = useState(false);

  // Valida o texto escrito e devolve o endereço normalizado (ou null se for inválido)
  function validar() {
    if (!url.trim()) {
      setMensagem({ tipo: 'erro', texto: 'Indica o endereço do servidor.' });
      return null;
    }
    const normalizado = normalizarUrl(url);
    if (!urlValido(normalizado)) {
      setMensagem({ tipo: 'erro', texto: 'Endereço inválido. Exemplo: http://192.168.1.50:8080' });
      return null;
    }
    return normalizado;
  }

  async function testar() {
    const normalizado = validar();
    if (!normalizado) return;
    setMensagem(null);
    setATestar(true);
    try {
      await axios.get(`${normalizado}/actuator/health`, { timeout: 5000 });
      setMensagem({ tipo: 'ok', texto: 'Ligação ao servidor bem-sucedida.' });
    } catch (e) {
      if (e.response) {
        // O servidor respondeu (mesmo que com erro), por isso está acessível
        setMensagem({ tipo: 'ok', texto: 'O servidor respondeu.' });
      } else {
        setMensagem({ tipo: 'erro', texto: 'Sem resposta. Confirma o IP, a porta e que o telemóvel está na mesma rede.' });
      }
    } finally {
      setATestar(false);
    }
  }

  async function guardar() {
    const normalizado = validar();
    if (!normalizado) return;
    try {
      await guardarUrl(normalizado);
      definirServidor(normalizado);
      navigation.goBack();
    } catch {
      setMensagem({ tipo: 'erro', texto: 'Não foi possível guardar o endereço.' });
    }
  }

  async function repor() {
    try {
      await reporUrl();
    } catch {
      // se falhar, continua com o valor por defeito apenas nesta sessão
    }
    definirServidor(URL_PADRAO);
    setUrl(URL_PADRAO);
    setMensagem({ tipo: 'ok', texto: 'Reposto o endereço por defeito.' });
  }

  return (
    <View style={s.ecra}>
      <Text style={s.explicacao}>
        Endereço do computador onde está a correr o backend. Muda sempre que trocas de rede (vê o IP com "ipconfig").
      </Text>

      <Text style={s.label}>Endereço do servidor</Text>
      <TextInput
        style={s.input}
        value={url}
        onChangeText={(t) => { setUrl(t); setMensagem(null); }}
        placeholder="http://192.168.1.50:8080"
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType="url"
      />

      {mensagem ? (
        <Text style={[s.mensagem, mensagem.tipo === 'ok' ? s.ok : s.erro]}>{mensagem.texto}</Text>
      ) : null}

      <View style={s.linha}>
        <Pressable style={[s.botao, s.secundario]} onPress={testar} disabled={aTestar}>
          {aTestar ? <ActivityIndicator color={cores.texto} /> : <Text style={s.textoSecundario}>Testar ligação</Text>}
        </Pressable>
        <Pressable style={[s.botao, s.primario]} onPress={guardar}>
          <Text style={s.textoPrimario}>Guardar</Text>
        </Pressable>
      </View>

      <Pressable style={s.repor} onPress={repor}>
        <Text style={s.reporTexto}>Repor endereço por defeito</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, padding: 20, backgroundColor: cores.fundo },
  explicacao: { fontSize: 14, lineHeight: 20, color: cores.textoSecundario, marginBottom: 20 },
  label: { fontSize: 14, fontWeight: '700', marginBottom: 6, color: cores.texto },
  input: {
    height: 52, borderWidth: 1.5, borderColor: cores.borda, borderRadius: 12,
    paddingHorizontal: 16, fontSize: 16, backgroundColor: cores.cartao, marginBottom: 16,
  },
  mensagem: { padding: 12, borderRadius: 10, marginBottom: 16 },
  ok: { color: cores.primaria, backgroundColor: cores.primariaSuave },
  erro: { color: cores.erro, backgroundColor: cores.erroSuave },
  linha: { flexDirection: 'row', gap: 10 },
  botao: { flex: 1, height: 52, borderRadius: 14, alignItems: 'center', justifyContent: 'center' },
  primario: { backgroundColor: cores.primaria },
  secundario: { borderWidth: 1.5, borderColor: cores.borda, backgroundColor: cores.cartao },
  textoPrimario: { color: '#fff', fontSize: 15, fontWeight: '700' },
  textoSecundario: { color: cores.texto, fontSize: 15, fontWeight: '700' },
  repor: { marginTop: 20, alignSelf: 'center', padding: 12 },
  reporTexto: { color: cores.textoSecundario, fontSize: 14, fontWeight: '600', textDecorationLine: 'underline' },
});
