import { View, Text, Pressable, StyleSheet } from 'react-native';
import { useAuth } from '../context/AuthContext';
import { cores } from '../tema';

export default function HomeScreen({ navigation }) {
  const { user, sair } = useAuth();
  return (
    <View style={s.ecra}>
      <Text style={s.ola}>Olá, {user?.nome ?? user?.username ?? 'utilizador'}</Text>

      <Pressable style={s.botao} onPress={() => navigation.navigate('Orcamentos')}>
        <Text style={s.botaoTexto}>Orçamentos</Text>
      </Pressable>

      <Pressable style={s.sair} onPress={sair}>
        <Text style={s.sairTexto}>Terminar sessão</Text>
      </Pressable>
    </View>
  );
}

const s = StyleSheet.create({
  ecra: { flex: 1, padding: 20, gap: 12, backgroundColor: cores.fundo },
  ola: { fontSize: 24, fontWeight: '800', color: cores.texto, marginBottom: 12 },
  botao: {
    height: 56, borderRadius: 14, backgroundColor: cores.cartao, borderWidth: 1,
    borderColor: cores.bordaSuave, justifyContent: 'center', paddingHorizontal: 18,
  },
  botaoTexto: { fontSize: 16, fontWeight: '700', color: cores.texto },
  sair: { marginTop: 'auto', height: 52, borderRadius: 14, borderWidth: 1.5, borderColor: '#E7B4AE', alignItems: 'center', justifyContent: 'center' },
  sairTexto: { color: cores.erro, fontSize: 15, fontWeight: '700' },
});
