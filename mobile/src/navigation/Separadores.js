import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import EcraProvisorioScreen from '../screens/EcraProvisorioScreen';
import HomeScreen from '../screens/HomeScreen';
import MaisScreen from '../screens/MaisScreen';
import { cores } from '../tema';

// Separadores da barra inferior, pela ordem em que aparecem (icones de Ionicons).
// Quem fizer o ecra real de Compras ou de Estatisticas troca aqui o EcraProvisorioScreen.
const SEPARADORES = [
  { nome: 'Inicio', titulo: 'Início', cabecalho: 'iShopping', icone: 'home', componente: HomeScreen },
  { nome: 'Compras', titulo: 'Compras', icone: 'cart', componente: EcraProvisorioScreen },
  { nome: 'Estatisticas', titulo: 'Estatísticas', icone: 'stats-chart', componente: EcraProvisorioScreen },
  { nome: 'Mais', titulo: 'Mais', icone: 'menu', componente: MaisScreen },
];

const Tab = createBottomTabNavigator();

export default function Separadores() {
  return (
    <Tab.Navigator
      screenOptions={{
        tabBarActiveTintColor: cores.primaria,
        tabBarInactiveTintColor: cores.textoSecundario,
        tabBarStyle: { backgroundColor: cores.cartao, borderTopColor: cores.bordaSuave },
        tabBarLabelStyle: { fontSize: 12, fontWeight: '600' },
        headerStyle: { backgroundColor: cores.cartao },
        headerTitleStyle: { color: cores.texto, fontWeight: '700' },
      }}
    >
      {SEPARADORES.map((separador) => (
        <Tab.Screen
          key={separador.nome}
          name={separador.nome}
          component={separador.componente}
          initialParams={{ titulo: separador.titulo }}
          options={{
            title: separador.cabecalho ?? separador.titulo,
            tabBarLabel: separador.titulo,
            tabBarIcon: ({ focused, color, size }) => (
              <Ionicons name={focused ? separador.icone : `${separador.icone}-outline`} size={size} color={color} />
            ),
          }}
        />
      ))}
    </Tab.Navigator>
  );
}
