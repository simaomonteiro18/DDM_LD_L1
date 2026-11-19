export const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

export function nomeDoMes(mes) {
  return MESES[mes - 1] ?? `Mês ${mes}`;
}

// 300 -> "300,00 €"
export function formatarEuro(valor) {
  const numero = Number(valor);
  if (Number.isNaN(numero)) return '—';
  return `${numero.toFixed(2).replace('.', ',')} €`;
}

// "2026-11-18T08:44:00Z" -> "18/11/2026 08:44"
export function formatarDataHora(texto) {
  if (!texto) return '';
  const d = new Date(texto);
  if (Number.isNaN(d.getTime())) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

// Instante (em ms) da mudanca de hora no ultimo domingo do mes, as 01:00 UTC (regra da UE)
function mudancaDeHora(ano, mesBase0) {
  const d = new Date(Date.UTC(ano, mesBase0 + 1, 0, 1));
  d.setUTCDate(d.getUTCDate() - d.getUTCDay());
  return d.getTime();
}

// "2026-07-07T23:30:00Z" -> "08/07": dia e mes em Lisboa, seja qual for o fuso do telemovel
export function formatarDiaMesLisboa(texto) {
  if (!texto) return '';
  const instante = new Date(texto).getTime();
  if (Number.isNaN(instante)) return '';
  const ano = new Date(instante).getUTCFullYear();
  const verao = instante >= mudancaDeHora(ano, 2) && instante < mudancaDeHora(ano, 9);
  const lisboa = new Date(instante + (verao ? 3600000 : 0));
  const p = (n) => String(n).padStart(2, '0');
  return `${p(lisboa.getUTCDate())}/${p(lisboa.getUTCMonth() + 1)}`;
}

// O utilizador pode vir como texto ("Ana") ou objeto ({ nome, username })
export function nomeUtilizador(valor) {
  if (!valor) return null;
  if (typeof valor === 'string') return valor;
  return valor.nome ?? valor.username ?? null;
}
