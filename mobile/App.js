import { useEffect, useState } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthProvider, useAuth } from './src/context/AuthContext';
import { definirServidor } from './src/api/client';
import { lerUrlGuardada } from './src/config/servidor';
import LoginScreen from './src/screens/LoginScreen';
import ServidorScreen from './src/screens/ServidorScreen';
import HomeScreen from './src/screens/HomeScreen';
import OrcamentosScreen from './src/screens/OrcamentosScreen';
import OrcamentoFormScreen from './src/screens/OrcamentoFormScreen';

const Stack = createNativeStackNavigator();

function Routes() {
  const { user } = useAuth();
  return (
    <Stack.Navigator>
      {user ? (
        <>
          <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'iShopping' }} />
          <Stack.Screen name="Orcamentos" component={OrcamentosScreen} options={{ title: 'Orçamentos' }} />
          <Stack.Screen name="OrcamentoForm" component={OrcamentoFormScreen} options={{ title: 'Orçamento' }} />
        </>
      ) : (
        <>
          <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
          <Stack.Screen name="Servidor" component={ServidorScreen} options={{ title: 'Servidor' }} />
        </>
      )}
    </Stack.Navigator>
  );
}

export default function App() {
  const [pronto, setPronto] = useState(false);

  // Antes de mostrar a app, lê o endereço do servidor guardado no telemóvel
  useEffect(() => {
    lerUrlGuardada().then((url) => {
      definirServidor(url);
      setPronto(true);
    });
  }, []);

  if (!pronto) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <AuthProvider>
      <NavigationContainer>
        <Routes />
      </NavigationContainer>
    </AuthProvider>
  );
}
