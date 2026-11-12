import { View, Text, Button } from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function HomeScreen() {
  const { user, sair } = useAuth();
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 }}>
      <Text style={{ fontSize: 20 }}>Olá, {user?.nome ?? user?.username ?? 'utilizador'}</Text>
      <Button title="Terminar sessão" onPress={sair} />
    </View>
  );
}