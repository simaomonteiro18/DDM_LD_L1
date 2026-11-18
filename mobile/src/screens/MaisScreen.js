import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../context/AuthContext';
import { ENTRADAS_GESTAO } from '../navigation/rotas';
import { cores } from '../tema';

// Separador Mais: utilizador autenticado, ecras de gestao e terminar sessao
export default function MaisScreen({ navigation }) {
  const { user, sair } = useAuth();
  return (
    <ScrollView style={s.fundo} contentContainerStyle={s.conteudo}>
      <View style={s.cartaoUtilizador}>
        <View style={s.avatar}>
          <Ionicons name="person" size={26} color={cores.primaria} />
        </View>
        <View style={s.dadosUtilizador}>
          <Text style={s.nome}>{user?.nome}</Text>
          <Text style={s.username}>@{user?.username}</Text>
        </View>
      </View>

      <Text style={s.seccao}>Gestão</Text>
      <View style={s.lista}>
        {ENTRADAS_GESTAO.map((entrada, indice) => (
          <Pressable
            key={entrada.nome}
            style={({ pressed }) => [s.linha, indice > 0 && s.linhaSeparada, pressed && s.premido]}
            onPress={() => navigation.navigate(entrada.nome)}
            accessibilityRole="button"
            accessibilityLabel={entrada.titulo}
          >
            <Ionicons name={entrada.icone} size={22} color={cores.primaria} />
            <Text style={s.linhaTexto}>{entrada.titulo}</Text>
            <Ionicons name="chevron-forward" size={20} color={cores.textoSecundario} />
          </Pressable>
        ))}
      </View>

      <Pressable
        style={({ pressed }) => [s.sair, pressed && s.premido]}
        onPress={sair}
        accessibilityRole="button"
      >
        <Ionicons name="log-out-outline" size={20} color={cores.erro} />
        <Text style={s.sairTexto}>Terminar sessão</Text>
      </Pressable>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  fundo: { flex: 1, backgroundColor: cores.fundo },
  conteudo: { padding: 16, paddingBottom: 32 },
  cartaoUtilizador: {
    flexDirection: 'row', alignItems: 'center', gap: 14, padding: 16, borderRadius: 14,
    backgroundColor: cores.cartao, borderWidth: 1, borderColor: cores.bordaSuave,
  },
  avatar: {
    width: 52, height: 52, borderRadius: 26, backgroundColor: cores.primariaSuave,
    alignItems: 'center', justifyContent: 'center',
  },
  dadosUtilizador: { flex: 1, gap: 2 },
  nome: { fontSize: 17, fontWeight: '700', color: cores.texto },
  username: { fontSize: 14, color: cores.textoSecundario },
  seccao: {
    fontSize: 13, fontWeight: '700', color: cores.textoSecundario, textTransform: 'uppercase',
    marginTop: 24, marginBottom: 8, marginLeft: 4,
  },
  lista: {
    borderRadius: 14, backgroundColor: cores.cartao, borderWidth: 1, borderColor: cores.bordaSuave, overflow: 'hidden',
  },
  linha: { flexDirection: 'row', alignItems: 'center', gap: 14, minHeight: 56, paddingHorizontal: 16 },
  linhaSeparada: { borderTopWidth: 1, borderTopColor: cores.bordaSuave },
  linhaTexto: { flex: 1, fontSize: 16, color: cores.texto, paddingVertical: 14 },
  sair: {
    flexDirection: 'row', gap: 8, marginTop: 28, minHeight: 52, borderRadius: 14, borderWidth: 1.5,
    borderColor: '#E7B4AE', alignItems: 'center', justifyContent: 'center',
  },
  sairTexto: { color: cores.erro, fontSize: 15, fontWeight: '700' },
  premido: { opacity: 0.6 },
});
