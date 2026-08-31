import React, { useState } from 'react';
import {
  Terminal,
  Activity,
  Send,
  Copy,
  Check,
  RotateCcw,
  Zap,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Code,
  FileText
} from 'lucide-react';
import { PingResult, RciCommandLog } from '../types';
import { routerService } from '../services/routerService';

interface DiagnosticsTabProps {
  onExecuteRci: (path: string, method: 'GET' | 'POST', body?: any) => Promise<any>;
}

export const DiagnosticsTab: React.FC<DiagnosticsTabProps> = ({ onExecuteRci }) => {
  // Ping State
  const [pingHost, setPingHost] = useState('8.8.8.8');
  const [pingCount, setPingCount] = useState(5);
  const [pingResult, setPingResult] = useState<PingResult | null>(null);
  const [isPinging, setIsPinging] = useState(false);

  // RCI Console State
  const [rciPath, setRciPath] = useState('/rci/show/system');
  const [rciMethod, setRciMethod] = useState<'GET' | 'POST'>('GET');
  const [rciBody, setRciBody] = useState('');
  const [rciResponse, setRciResponse] = useState<any>(null);
  const [isExecutingRci, setIsExecutingRci] = useState(false);
  const [rciDuration, setRciDuration] = useState<number | null>(null);
  const [rciStatus, setRciStatus] = useState<number | null>(null);
  const [isCopied, setIsCopied] = useState(false);

  const presetRciEndpoints = [
    { label: 'System Status', path: '/rci/show/system', method: 'GET' as const },
    { label: 'Interfaces', path: '/rci/show/interface', method: 'GET' as const },
    { label: 'Hotspot Clients', path: '/rci/show/ip/hotspot', method: 'GET' as const },
    { label: 'Keenetic Version', path: '/rci/show/version', method: 'GET' as const },
    { label: 'IP Routing Table', path: '/rci/show/ip/route', method: 'GET' as const },
    { label: 'DNS Cache', path: '/rci/show/ip/name-server', method: 'GET' as const },
  ];

  const handleRunPing = async () => {
    setIsPinging(true);
    try {
      const result = await routerService.runPingTest(pingHost, pingCount);
      setPingResult(result);
    } catch (err) {
      console.error('Ping failed', err);
    } finally {
      setIsPinging(false);
    }
  };

  const handleExecuteRci = async () => {
    setIsExecutingRci(true);
    setRciResponse(null);
    const start = Date.now();
    try {
      let parsedBody: any = undefined;
      if (rciMethod === 'POST' && rciBody.trim()) {
        try {
          parsedBody = JSON.parse(rciBody);
        } catch {
          parsedBody = rciBody;
        }
      }

      const res = await onExecuteRci(rciPath, rciMethod, parsedBody);
      setRciResponse(res);
      setRciStatus(200);
      setRciDuration(Date.now() - start);
    } catch (err: any) {
      setRciResponse({ error: err.message || 'RCI Execution Failed' });
      setRciStatus(500);
      setRciDuration(Date.now() - start);
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
      {/* Top Banner */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Zap className="w-5 h-5 text-blue-400" />
            Сетевая диагностика и RCI REST API Консоль
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Прямое взаимодействие с ядром KeeneticOS через RCI эндпоинты и утилиты проверки сетевой задержки (Ping/ICMP)
          </p>
        </div>
      </div>

      {/* Ping Diagnostic Tool */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
            <Activity className="w-4 h-4 text-emerald-400" />
            Проверка доступности и задержки (Ping / ICMP)
          </h3>
          <span className="text-xs text-zinc-400 font-mono">Keenetic NetTools</span>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="flex-1 w-full relative">
            <input
              type="text"
              value={pingHost}
              onChange={e => setPingHost(e.target.value)}
              placeholder="Введите IP адрес или домен (например, 8.8.8.8, 1.1.1.1, ya.ru)"
              className="w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs font-mono text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          <div className="flex items-center space-x-2 w-full sm:w-auto">
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
        </div>

        {/* Quick Target Chips */}
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="text-zinc-400 self-center text-[11px]">Быстрый выбор:</span>
          {[
            { name: 'Google DNS (8.8.8.8)', host: '8.8.8.8' },
            { name: 'Cloudflare (1.1.1.1)', host: '1.1.1.1' },
            { name: 'Yandex (ya.ru)', host: 'ya.ru' },
            { name: 'Шлюз (94.25.180.1)', host: '94.25.180.1' },
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

        {/* Ping Results Display */}
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

            {/* Packet History Bars */}
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

      {/* RCI REST API Console */}
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
          {presetRciEndpoints.map(preset => (
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

        {/* Path and Method Input */}
        <div className="flex flex-col sm:flex-row items-center gap-2">
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
            id="execute-rci-btn"
            onClick={handleExecuteRci}
            disabled={isExecutingRci}
            className="w-full sm:w-auto px-6 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition flex items-center justify-center space-x-2 cursor-pointer shadow whitespace-nowrap"
          >
            <Send className={`w-3.5 h-3.5 ${isExecutingRci ? 'animate-spin' : ''}`} />
            <span>{isExecutingRci ? 'Выполнение...' : 'Выполнить RCI'}</span>
          </button>
        </div>

        {/* POST Body Editor if POST selected */}
        {rciMethod === 'POST' && (
          <div>
            <label className="block text-xs font-medium text-zinc-400 mb-1">
              JSON Body (Параметры команды)
            </label>
            <textarea
              rows={3}
              value={rciBody}
              onChange={e => setRciBody(e.target.value)}
              placeholder='{"name": "WifiMaster0/AccessPoint0", "up": true}'
              className="w-full px-3.5 py-2 rounded-lg bg-[#09090b] border border-[#27272a] font-mono text-xs text-white focus:outline-none focus:border-blue-500"
            />
          </div>
        )}

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
    </div>
  );
};
