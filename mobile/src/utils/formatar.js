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

// O utilizador pode vir como texto ("Ana") ou objeto ({ nome, username })
export function nomeUtilizador(valor) {
  if (!valor) return null;
  if (typeof valor === 'string') return valor;
  return valor.nome ?? valor.username ?? null;
}
