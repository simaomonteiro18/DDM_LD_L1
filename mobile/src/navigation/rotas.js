import EcraProvisorioScreen from '../screens/EcraProvisorioScreen';
import OrcamentosScreen from '../screens/OrcamentosScreen';
import OrcamentoFormScreen from '../screens/OrcamentoFormScreen';
import TiposArtigoScreen from '../screens/TiposArtigoScreen';
import TipoArtigoFormScreen from '../screens/TipoArtigoFormScreen';
import ArtigosScreen from '../screens/ArtigosScreen';
import ArtigoFormScreen from '../screens/ArtigoFormScreen';
import UtilizadoresScreen from '../screens/UtilizadoresScreen';
import UtilizadorFormScreen from '../screens/UtilizadorFormScreen';

// Registo central dos ecras que abrem em pilha por cima dos separadores (os separadores estao em Separadores.js).
// Cada pessoa troca o EcraProvisorioScreen da sua entrada pelo ecra real quando o fundir
// (importa o ecra no topo deste ficheiro e muda o campo "componente").

// Entradas da seccao "Gestao" do separador Mais, pela ordem em que aparecem (icones de Ionicons)
export const ENTRADAS_GESTAO = [
  { nome: 'TiposArtigo', titulo: 'Tipos de artigo', icone: 'pricetags-outline', componente: TiposArtigoScreen },
  { nome: 'Artigos', titulo: 'Artigos', icone: 'basket-outline', componente: ArtigosScreen },
  { nome: 'Orcamentos', titulo: 'Orçamentos mensais', icone: 'wallet-outline', componente: OrcamentosScreen },
  { nome: 'Utilizadores', titulo: 'Utilizadores', icone: 'people-outline', componente: UtilizadoresScreen },
];

// Ecras abertos a partir de outros ecras, que nao aparecem em nenhum menu
export const OUTRAS_ROTAS = [
  { nome: 'ModoCompra', titulo: 'Modo Compra', componente: EcraProvisorioScreen },
  { nome: 'EditarCompra', titulo: 'Editar compra', componente: EcraProvisorioScreen },
  { nome: 'Planeamento', titulo: 'Planeamento de Compras', componente: EcraProvisorioScreen },
  { nome: 'OrcamentoForm', titulo: 'Orçamento', componente: OrcamentoFormScreen },
  { nome: 'TipoArtigoForm', titulo: 'Tipo de artigo', componente: TipoArtigoFormScreen },
  { nome: 'ArtigoForm', titulo: 'Artigo', componente: ArtigoFormScreen },
  { nome: 'UtilizadorForm', titulo: 'Utilizador', componente: UtilizadorFormScreen },
];

export const ROTAS_EMPILHADAS = [...ENTRADAS_GESTAO, ...OUTRAS_ROTAS];
