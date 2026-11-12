import { View, Text, Button } from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function HomeScreen() {
  const { user, setUser } = useAuth();
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12 }}>
      <Text style={{ fontSize: 20 }}>Olá, {user?.nome}</Text>
      <Button title="Terminar sessão" onPress={() => setUser(null)} />
    </View>
  );
}