import EcraProvisorioScreen from '../screens/EcraProvisorioScreen';

// Entradas do menu do ecra principal, pela ordem em que aparecem.
// Cada pessoa troca o EcraProvisorioScreen da sua entrada pelo ecra real quando o fundir
// (importa o ecra no topo deste ficheiro e muda o campo "componente").
export const ENTRADAS_MENU = [
  { nome: 'TiposArtigo', titulo: 'Tipos de Artigo', descricao: 'Categorias dos artigos', componente: EcraProvisorioScreen },
  { nome: 'Artigos', titulo: 'Artigos', descricao: 'Produtos a comprar', componente: EcraProvisorioScreen },
  { nome: 'Orcamentos', titulo: 'Orçamentos', descricao: 'Valor previsto por mês', componente: EcraProvisorioScreen },
  { nome: 'Utilizadores', titulo: 'Utilizadores', descricao: 'Quem usa a app', componente: EcraProvisorioScreen },
  { nome: 'Planeamento', titulo: 'Planeamento de Compras', descricao: 'Preparar as próximas compras', componente: EcraProvisorioScreen },
  { nome: 'Estatisticas', titulo: 'Estatísticas', descricao: 'Gastos e sugestões', componente: EcraProvisorioScreen },
];

// Ecras abertos a partir de outros ecras, que nao aparecem no menu
export const OUTRAS_ROTAS = [
  { nome: 'ModoCompra', titulo: 'Modo Compra', componente: EcraProvisorioScreen },
];

// Todos os ecras disponiveis depois do login (alem do ecra principal)
export const ROTAS_AUTENTICADAS = [...ENTRADAS_MENU, ...OUTRAS_ROTAS];
