import React, { useState } from 'react';
import {
  Zap,
  Activity,
  Send,
  Terminal,
  Copy,
  Check,
  Code,
  Network,
  ArrowRight,
  Server,
  Cpu
} from 'lucide-react';
import { PingResult, TracerouteResult } from '../types';
import { routerService } from '../services/routerService';

interface DiagnosticsTabProps {
  onExecuteRci: (path: string, method: 'GET' | 'POST', body?: any) => Promise<any>;
}

export const DiagnosticsTab: React.FC<DiagnosticsTabProps> = ({
  onExecuteRci,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'ping' | 'traceroute' | 'rci'>('ping');

  // Ping state
  const [pingHost, setPingHost] = useState<string>('8.8.8.8');
  const [pingCount, setPingCount] = useState<number>(4);
  const [isPinging, setIsPinging] = useState<boolean>(false);
  const [pingResult, setPingResult] = useState<PingResult | null>(null);

  // Traceroute state
  const [traceHost, setTraceHost] = useState<string>('8.8.8.8');
  const [isTracing, setIsTracing] = useState<boolean>(false);
  const [traceResult, setTraceResult] = useState<TracerouteResult | null>(null);

  // RCI Console state
  const [rciPath, setRciPath] = useState<string>('/rci/show/system');
  const [rciMethod, setRciMethod] = useState<'GET' | 'POST'>('GET');
  const [rciBody, setRciBody] = useState<string>('');
  const [isExecutingRci, setIsExecutingRci] = useState<boolean>(false);
  const [rciResponse, setRciResponse] = useState<any>(null);
  const [rciStatus, setRciStatus] = useState<number | null>(null);
  const [rciDuration, setRciDuration] = useState<number | null>(null);
  const [isCopied, setIsCopied] = useState<boolean>(false);

  // Ping handler
  const handleRunPing = async () => {
    if (!pingHost) return;
    setIsPinging(true);
    try {
      const res = await routerService.runPingTest(pingHost, pingCount);
      setPingResult(res);
    } catch (err) {
      console.error('Ping test error:', err);
    } finally {
      setIsPinging(false);
    }
  };

  // Traceroute handler
  const handleRunTraceroute = async () => {
    if (!traceHost) return;
    setIsTracing(true);
    try {
      const res = await routerService.runTraceroute(traceHost);
      setTraceResult(res);
    } catch (err) {
      console.error('Traceroute error:', err);
    } finally {
      setIsTracing(false);
    }
  };

  // RCI presets
  const rciPresets = [
    { label: 'System Info', path: '/rci/show/system', method: 'GET' as const },
    { label: 'Interfaces', path: '/rci/show/interface', method: 'GET' as const },
    { label: 'Hotspot Clients', path: '/rci/show/ip/hotspot', method: 'GET' as const },
    { label: 'Routing Table', path: '/rci/show/ip/route', method: 'GET' as const },
    { label: 'Firmware Version', path: '/rci/show/version', method: 'GET' as const },
  ];

  // Execute RCI
  const handleExecuteRci = async () => {
    if (!rciPath) return;
    setIsExecutingRci(true);
    setRciResponse(null);
    setRciStatus(null);
    const start = performance.now();

    try {
      let bodyObj = undefined;
      if (rciMethod === 'POST' && rciBody) {
        try {
          bodyObj = JSON.parse(rciBody);
        } catch {
          bodyObj = rciBody;
        }
      }

      const res = await onExecuteRci(rciPath, rciMethod, bodyObj);
      const end = performance.now();
      setRciDuration(Math.round(end - start));
      setRciResponse(res);
      setRciStatus(200);
    } catch (err: any) {
      const end = performance.now();
      setRciDuration(Math.round(end - start));
      setRciResponse({ error: err.message || 'RCI execution error' });
      setRciStatus(500);
    } finally {
      setIsExecutingRci(false);
    }
  };

  const copyRciResponse = () => {
    if (!rciResponse) return;
    navigator.clipboard.writeText(JSON.stringify(rciResponse, null, 2));
    setIsCopied(true);
    setTimeout(() => setIsCopied(false), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Zap className="w-5 h-5 text-blue-400" />
            Сетевая диагностика и RCI REST API Консоль
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Инструменты проверки задержки, трассировки маршрутов пакетов и прямого взаимодействия с ядром KeeneticOS
          </p>
        </div>

        {/* Sub-tab Switcher */}
        <div className="flex items-center space-x-1 p-1 bg-[#09090b] border border-[#27272a] rounded-lg text-xs self-start sm:self-auto">
          <button
            onClick={() => setActiveSubTab('ping')}
            className={`px-3 py-1.5 rounded-md font-medium transition cursor-pointer ${
              activeSubTab === 'ping'
                ? 'bg-blue-600 text-white font-semibold shadow-xs'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Ping (ICMP)
          </button>
          <button
            onClick={() => setActiveSubTab('traceroute')}
            className={`px-3 py-1.5 rounded-md font-medium transition cursor-pointer ${
              activeSubTab === 'traceroute'
                ? 'bg-blue-600 text-white font-semibold shadow-xs'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            Трассировка
          </button>
          <button
            onClick={() => setActiveSubTab('rci')}
            className={`px-3 py-1.5 rounded-md font-medium transition cursor-pointer ${
              activeSubTab === 'rci'
                ? 'bg-blue-600 text-white font-semibold shadow-xs'
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            RCI Консоль
          </button>
        </div>
      </div>

      {/* 1. PING TOOL */}
      {activeSubTab === 'ping' && (
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
              <Activity className="w-4 h-4 text-emerald-400" />
              Проверка доступности и задержки (Ping / ICMP)
            </h3>
            <span className="text-xs text-zinc-400 font-mono">Keenetic NetTools</span>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="flex-1 w-full">
              <input
                type="text"
                value={pingHost}
                onChange={e => setPingHost(e.target.value)}
                placeholder="Введите IP адрес или домен (например, 8.8.8.8, 1.1.1.1, ya.ru)"
                className="w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs font-mono text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
              />
            </div>

            <select
              value={pingCount}
              onChange={e => setPingCount(Number(e.target.value))}
              className="px-3 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs text-zinc-300 focus:outline-none cursor-pointer"
            >
              <option value={3}>3 пакета</option>
              <option value={5}>5 пакетов</option>
              <option value={10}>10 пакетов</option>
            </select>

            <button
              onClick={handleRunPing}
              disabled={isPinging}
              className="px-5 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition flex items-center space-x-2 cursor-pointer shadow whitespace-nowrap"
            >
              <Send className={`w-3.5 h-3.5 ${isPinging ? 'animate-spin' : ''}`} />
              <span>{isPinging ? 'Проверка...' : 'Запустить Ping'}</span>
            </button>
          </div>

          {/* Quick Target Chips */}
          <div className="flex flex-wrap gap-2 text-xs">
            <span className="text-zinc-400 self-center text-[11px]">Быстрый выбор:</span>
            {[
              { name: 'Google DNS (8.8.8.8)', host: '8.8.8.8' },
              { name: 'Cloudflare (1.1.1.1)', host: '1.1.1.1' },
              { name: 'Яндекс (77.88.8.8)', host: '77.88.8.8' },
              { name: 'Шлюз провайдера', host: '94.25.180.1' },
            ].map(t => (
              <button
                key={t.host}
                onClick={() => setPingHost(t.host)}
                className="px-2.5 py-1 rounded-md bg-[#09090b] hover:bg-[#27272a] text-zinc-300 text-[11px] font-mono border border-[#27272a] cursor-pointer transition"
              >
                {t.name}
              </button>
            ))}
          </div>

          {/* Ping Results */}
          {pingResult && (
            <div className="p-4 rounded-xl bg-[#09090b] border border-[#27272a] space-y-3">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div className="p-2.5 rounded-lg bg-[#18181b] border border-[#27272a]">
                  <span className="text-zinc-400 block text-[11px]">Средняя задержка</span>
                  <span className="text-lg font-mono font-bold text-emerald-400">{pingResult.avgMs} мс</span>
                </div>
                <div className="p-2.5 rounded-lg bg-[#18181b] border border-[#27272a]">
                  <span className="text-zinc-400 block text-[11px]">Минимальная</span>
                  <span className="text-lg font-mono font-bold text-blue-400">{pingResult.minMs} мс</span>
                </div>
                <div className="p-2.5 rounded-lg bg-[#18181b] border border-[#27272a]">
                  <span className="text-zinc-400 block text-[11px]">Максимальная</span>
                  <span className="text-lg font-mono font-bold text-amber-400">{pingResult.maxMs} мс</span>
                </div>
                <div className="p-2.5 rounded-lg bg-[#18181b] border border-[#27272a]">
                  <span className="text-zinc-400 block text-[11px]">Потери пакетов</span>
                  <span className="text-lg font-mono font-bold text-emerald-400">{pingResult.loss}%</span>
                </div>
              </div>

              {/* Packet Responses */}
              <div className="flex items-center space-x-2 pt-2 text-xs text-zinc-400">
                <span>Отклики пакетов:</span>
                <div className="flex items-center space-x-1.5 font-mono">
                  {pingResult.history.map((ms, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-[#18181b] text-blue-300 font-bold border border-[#27272a]"
                    >
                      #{idx + 1}: {ms}ms
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 2. TRACEROUTE TOOL */}
      {activeSubTab === 'traceroute' && (
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
              <Network className="w-4 h-4 text-blue-400" />
              Трассировка маршрута (Traceroute)
            </h3>
            <span className="text-xs text-zinc-400 font-mono">Max 30 Hops</span>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="flex-1 w-full">
              <input
                type="text"
                value={traceHost}
                onChange={e => setTraceHost(e.target.value)}
                placeholder="Хост или IP адрес для трассировки (например: google.com, 8.8.8.8)"
                className="w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs font-mono text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
              />
            </div>

            <button
              onClick={handleRunTraceroute}
              disabled={isTracing}
              className="px-5 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition flex items-center space-x-2 cursor-pointer shadow whitespace-nowrap"
            >
              <Send className={`w-3.5 h-3.5 ${isTracing ? 'animate-spin' : ''}`} />
              <span>{isTracing ? 'Трассировка...' : 'Начать трассировку'}</span>
            </button>
          </div>

          {/* Traceroute Table */}
          {traceResult && (
            <div className="rounded-xl bg-[#09090b] border border-[#27272a] overflow-hidden">
              <div className="px-4 py-2.5 bg-[#18181b] border-b border-[#27272a] flex items-center justify-between text-xs">
                <span className="font-semibold text-white">Маршрут до: {traceResult.target}</span>
                <span className="text-emerald-400 font-mono">Успешно завершено ({traceResult.hops.length} хопов)</span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs font-mono">
                  <thead className="bg-[#09090b] text-zinc-400 border-b border-[#27272a]">
                    <tr>
                      <th className="py-2.5 px-4"># Хоп</th>
                      <th className="py-2.5 px-4">Узел / Имя хоста</th>
                      <th className="py-2.5 px-4">IP Адрес</th>
                      <th className="py-2.5 px-4">RTT 1</th>
                      <th className="py-2.5 px-4">RTT 2</th>
                      <th className="py-2.5 px-4">RTT 3</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#27272a]">
                    {traceResult.hops.map(h => (
                      <tr key={h.hop} className="hover:bg-[#18181b]/60 transition">
                        <td className="py-2.5 px-4 font-bold text-blue-400">{h.hop}</td>
                        <td className="py-2.5 px-4 text-zinc-200">{h.host}</td>
                        <td className="py-2.5 px-4 text-zinc-400">{h.ip}</td>
                        <td className="py-2.5 px-4 text-emerald-400">{h.rtt1} ms</td>
                        <td className="py-2.5 px-4 text-emerald-400">{h.rtt2} ms</td>
                        <td className="py-2.5 px-4 text-emerald-400">{h.rtt3} ms</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 3. RCI REST API CONSOLE */}
      {activeSubTab === 'rci' && (
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
              <Terminal className="w-4 h-4 text-blue-400" />
              Интерактивная RCI REST API Консоль
            </h3>
            <span className="text-xs text-blue-400 font-mono">Keenetic RCI Spec</span>
          </div>

          {/* Preset Buttons */}
          <div className="flex flex-wrap gap-2">
            {rciPresets.map(preset => (
              <button
                key={preset.path}
                onClick={() => {
                  setRciPath(preset.path);
                  setRciMethod(preset.method);
                }}
                className={`px-3 py-1.5 rounded-md text-xs font-mono font-medium transition cursor-pointer border ${
                  rciPath === preset.path
                    ? 'bg-blue-500/15 text-blue-300 border-blue-500/40'
                    : 'bg-[#09090b] hover:bg-[#27272a] text-zinc-300 border-[#27272a]'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>

          {/* Request Input Bar */}
          <div className="flex flex-col sm:flex-row items-center gap-2.5">
            <select
              value={rciMethod}
              onChange={e => setRciMethod(e.target.value as any)}
              className="w-full sm:w-28 px-3 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs font-bold font-mono text-blue-400 focus:outline-none cursor-pointer"
            >
              <option value="GET">GET</option>
              <option value="POST">POST</option>
            </select>

            <input
              type="text"
              value={rciPath}
              onChange={e => setRciPath(e.target.value)}
              placeholder="/rci/show/system"
              className="flex-1 w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs font-mono text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
            />

            <button
              onClick={handleExecuteRci}
              disabled={isExecutingRci}
              className="w-full sm:w-auto px-6 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition flex items-center justify-center space-x-2 cursor-pointer shadow whitespace-nowrap"
            >
              <Send className={`w-3.5 h-3.5 ${isExecutingRci ? 'animate-spin' : ''}`} />
              <span>{isExecutingRci ? 'Выполнение...' : 'Выполнить RCI'}</span>
            </button>
          </div>

          {/* Response Viewer */}
          {rciResponse && (
            <div className="rounded-xl bg-[#09090b] border border-[#27272a] overflow-hidden shadow-inner">
              <div className="flex items-center justify-between px-4 py-2.5 bg-[#18181b] border-b border-[#27272a] text-xs">
                <div className="flex items-center space-x-3">
                  <span className="font-semibold text-white">Ответ KeeneticOS RCI:</span>
                  {rciStatus && (
                    <span
                      className={`px-2 py-0.5 rounded font-mono font-bold text-[11px] ${
                        rciStatus === 200
                          ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20'
                          : 'bg-rose-500/15 text-rose-400 border border-rose-500/20'
                      }`}
                    >
                      HTTP {rciStatus}
                    </span>
                  )}
                  {rciDuration !== null && (
                    <span className="text-zinc-400 font-mono text-[11px]">{rciDuration} мс</span>
                  )}
                </div>

                <button
                  onClick={copyRciResponse}
                  className="flex items-center space-x-1 text-zinc-400 hover:text-white px-2 py-1 rounded bg-[#27272a] hover:bg-[#3f3f46] transition cursor-pointer font-medium"
                >
                  {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{isCopied ? 'Скопировано' : 'Копировать JSON'}</span>
                </button>
              </div>

              <pre className="p-4 text-xs font-mono text-blue-300 overflow-x-auto max-h-96 leading-relaxed">
                {JSON.stringify(rciResponse, null, 2)}
              </pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
