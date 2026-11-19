import { useEffect, useState } from 'react';
import { View, ActivityIndicator } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthProvider, useAuth } from './src/context/AuthContext';
import { definirServidor } from './src/api/client';
import { lerUrlGuardada } from './src/config/servidor';
import LoginScreen from './src/screens/LoginScreen';
import ServidorScreen from './src/screens/ServidorScreen';
import Separadores from './src/navigation/Separadores';
import { ROTAS_EMPILHADAS } from './src/navigation/rotas';

const Stack = createNativeStackNavigator();

function Routes() {
  const { user } = useAuth();
  return (
    <Stack.Navigator>
      {user ? (
        <>
          {/* Os separadores ficam por baixo; os ecras de gestao abrem em pilha por cima deles */}
          <Stack.Screen name="Separadores" component={Separadores} options={{ headerShown: false }} />
          {ROTAS_EMPILHADAS.map((rota) => (
            <Stack.Screen
              key={rota.nome}
              name={rota.nome}
              component={rota.componente}
              options={{ title: rota.titulo }}
              initialParams={{ titulo: rota.titulo }}
            />
          ))}
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
