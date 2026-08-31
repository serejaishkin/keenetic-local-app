import React, { useState, useEffect } from 'react';
import {
  FileText,
  Search,
  Filter,
  Trash2,
  Download,
  Copy,
  Check,
  RefreshCw,
  Clock,
  Terminal,
  Shield,
  Radio,
  Wifi,
  Cpu
} from 'lucide-react';
import { SyslogEntry } from '../types';
import { routerService } from '../services/routerService';

export const SystemLogTab: React.FC = () => {
  const [logs, setLogs] = useState<SyslogEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedFacility, setSelectedFacility] = useState<string>('all');
  const [selectedLevel, setSelectedLevel] = useState<string>('all');
  const [autoRefresh, setAutoRefresh] = useState<boolean>(true);
  const [isCopied, setIsCopied] = useState<boolean>(false);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  const fetchLogs = async () => {
    setIsRefreshing(true);
    const data = await routerService.getSyslog();
    setLogs(data);
    setTimeout(() => setIsRefreshing(false), 300);
  };

  useEffect(() => {
    fetchLogs();

    if (!autoRefresh) return;
    const interval = setInterval(() => {
      fetchLogs();
    }, 3000);
    return () => clearInterval(interval);
  }, [autoRefresh]);

  const handleClear = async () => {
    await routerService.clearSyslog();
    setLogs([]);
  };

  const handleCopy = () => {
    const text = logs.map(l => `[${l.timestamp}] [${l.level}] [${l.facility}]: ${l.message}`).join('\n');
    navigator.clipboard.writeText(text);
    setIsCopied(true);
    setTimeout(() => setIsCopied(false), 2000);
  };

  const handleDownload = () => {
    const text = logs.map(l => `[${l.timestamp}] [${l.level}] [${l.facility}]: ${l.message}`).join('\n');
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `keenetic-syslog-${new Date().toISOString().slice(0, 10)}.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };

  // Facilities list
  const facilities = [
    { id: 'all', label: 'Все службы' },
    { id: 'ndm', label: 'NDM Ядро' },
    { id: 'dhcpd', label: 'DHCP' },
    { id: 'kernel', label: 'Kernel' },
    { id: 'wpa_supplicant', label: 'Wi-Fi' },
    { id: 'wireguard', label: 'WireGuard VPN' },
    { id: 'dnsmasq', label: 'DNS' },
  ];

  // Filter logs
  const filteredLogs = logs.filter(log => {
    const matchesFacility = selectedFacility === 'all' || log.facility === selectedFacility;
    const matchesLevel = selectedLevel === 'all' || log.level === selectedLevel;
    const matchesSearch = searchQuery === '' ||
      log.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.facility.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.timestamp.includes(searchQuery);
    return matchesFacility && matchesLevel && matchesSearch;
  });

  const getLevelBadge = (level: SyslogEntry['level']) => {
    switch (level) {
      case 'ERROR':
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-rose-500/15 text-rose-400 border border-rose-500/30">ERR</span>;
      case 'WARNING':
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/15 text-amber-400 border border-amber-500/30">WARN</span>;
      case 'NOTICE':
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-blue-500/15 text-blue-400 border border-blue-500/30">NOTE</span>;
      default:
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-medium bg-zinc-800 text-zinc-300 border border-[#27272a]">INFO</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Info Banner */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <FileText className="w-5 h-5 text-blue-400" />
            Журнал событий и системный лог (KeeneticOS Syslog)
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Непрерывный мониторинг системных событий ядра NDM, аренды DHCP, подключений Wi-Fi клиентов и правил безопасности
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer border flex items-center space-x-1.5 ${
              autoRefresh
                ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                : 'bg-[#09090b] text-zinc-400 border-[#27272a]'
            }`}
          >
            <span className={`w-2 h-2 rounded-full ${autoRefresh ? 'bg-emerald-400 animate-pulse' : 'bg-zinc-600'}`} />
            <span>Автообновление (3с)</span>
          </button>

          <button
            onClick={fetchLogs}
            disabled={isRefreshing}
            className="p-2 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-300 hover:text-white transition cursor-pointer"
            title="Обновить журнал"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter and Action Controls */}
      <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col lg:flex-row items-center justify-between gap-3 text-xs">
        {/* Search */}
        <div className="relative w-full lg:w-72">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Поиск по тексту события, IP или MAC..."
            className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Facility & Level Filter Tabs */}
        <div className="flex flex-wrap items-center gap-2 w-full lg:w-auto">
          {facilities.map(f => (
            <button
              key={f.id}
              onClick={() => setSelectedFacility(f.id)}
              className={`px-2.5 py-1 rounded-md font-medium transition cursor-pointer text-[11px] border ${
                selectedFacility === f.id
                  ? 'bg-blue-600/15 text-blue-400 border-blue-500/40'
                  : 'bg-[#09090b] text-zinc-400 border-[#27272a] hover:text-zinc-200'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-2 shrink-0 self-end lg:self-auto">
          <button
            onClick={handleCopy}
            className="px-3 py-1.5 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-300 hover:text-white transition flex items-center space-x-1.5 cursor-pointer"
          >
            {isCopied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{isCopied ? 'Скопировано' : 'Копировать'}</span>
          </button>

          <button
            onClick={handleDownload}
            className="px-3 py-1.5 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-300 hover:text-white transition flex items-center space-x-1.5 cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-blue-400" />
            <span>Экспорт .txt</span>
          </button>

          <button
            onClick={handleClear}
            className="p-1.5 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-400 hover:text-rose-400 transition cursor-pointer"
            title="Очистить лог"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Syslog Table / Stream View */}
      <div className="rounded-xl bg-[#18181b] border border-[#27272a] overflow-hidden shadow-sm">
        <div className="px-4 py-3 bg-[#09090b] border-b border-[#27272a] flex items-center justify-between text-xs">
          <div className="flex items-center space-x-2">
            <Terminal className="w-4 h-4 text-blue-400" />
            <span className="font-semibold text-white">Журнал KeeneticOS</span>
            <span className="text-zinc-500">({filteredLogs.length} записей)</span>
          </div>
          <div className="flex items-center space-x-2 text-[11px] font-mono text-zinc-400">
            <span>Буфер: 512 KB</span>
          </div>
        </div>

        <div className="p-3 bg-[#09090b] font-mono text-xs overflow-x-auto max-h-[540px] overflow-y-auto space-y-1 divide-y divide-[#27272a]/40">
          {filteredLogs.length > 0 ? (
            filteredLogs.map(log => (
              <div
                key={log.id}
                className="pt-1.5 pb-1 flex flex-col sm:flex-row items-start sm:items-center gap-2 hover:bg-[#18181b]/50 px-2 rounded transition"
              >
                <div className="flex items-center space-x-2 shrink-0">
                  <span className="text-zinc-500 text-[11px]">{log.timestamp}</span>
                  {getLevelBadge(log.level)}
                  <span className="px-1.5 py-0.5 rounded bg-[#18181b] text-[11px] text-zinc-400 border border-[#27272a]">
                    {log.facility}
                  </span>
                </div>
                <div className="text-zinc-200 break-all text-xs pl-1">
                  {log.message}
                </div>
              </div>
            ))
          ) : (
            <div className="p-8 text-center text-zinc-500 font-sans">
              Записи по заданному фильтру отсутствуют
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
