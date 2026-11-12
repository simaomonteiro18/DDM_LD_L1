import { View, Button } from 'react-native';
import { useAuth } from '../context/AuthContext';

export default function LoginScreen() {
  const { setUser } = useAuth();
  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 24 }}>
      <Button title="Entrar (teste)" onPress={() => setUser({ id: 1, nome: 'Teste' })} />
    </View>
  );
}