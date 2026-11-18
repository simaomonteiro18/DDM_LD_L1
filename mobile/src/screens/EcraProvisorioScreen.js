import { ScrollView, Text, StyleSheet } from 'react-native';
import { cores } from '../tema';

// Ecra generico usado enquanto o ecra real de uma entrada nao existe
export default function EcraProvisorioScreen({ route }) {
  const { titulo, detalhe } = route.params ?? {};
  return (
    <ScrollView style={s.fundo} contentContainerStyle={s.conteudo}>
      <Text style={s.titulo}>{titulo}</Text>
      {detalhe ? <Text style={s.detalhe}>{detalhe}</Text> : null}
      <Text style={s.texto}>Em desenvolvimento</Text>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  fundo: { flex: 1, backgroundColor: cores.fundo },
  conteudo: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: 24, gap: 10 },
  titulo: { fontSize: 24, fontWeight: '800', textAlign: 'center', color: cores.texto },
  detalhe: { fontSize: 16, textAlign: 'center', color: cores.texto },
  texto: { fontSize: 15, textAlign: 'center', color: cores.textoSecundario },
});
