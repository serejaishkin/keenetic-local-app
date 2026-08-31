import React, { useState } from 'react';
import {
  Cpu,
  HardDrive,
  Globe,
  Clock,
  ArrowDownCircle,
  ArrowUpCircle,
  Power,
  RotateCcw,
  Wifi,
  ShieldCheck,
  CheckCircle,
  AlertCircle,
  Radio,
  Server,
  Zap
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid
} from 'recharts';
import {
  SystemStatus,
  WanStatus,
  WifiInterface,
  ClientDevice,
  PortStatus,
  TrafficPoint
} from '../types';
import { formatBytes } from '../services/routerService';

interface DashboardTabProps {
  system: SystemStatus | null;
  wan: WanStatus | null;
  wifiList: WifiInterface[];
  clients: ClientDevice[];
  ports: PortStatus[];
  trafficHistory: TrafficPoint[];
  onToggleWifi: (id: string, enabled: boolean) => void;
  onReboot: () => void;
  onReconnectWan: () => void;
  onNavigateToTab: (tabId: string) => void;
}

export const DashboardTab: React.FC<DashboardTabProps> = ({
  system,
  wan,
  wifiList,
  clients,
  ports,
  trafficHistory,
  onToggleWifi,
  onReboot,
  onReconnectWan,
  onNavigateToTab,
}) => {
  const [showRebootConfirm, setShowRebootConfirm] = useState(false);
  const [isRebooting, setIsRebooting] = useState(false);
  const [isReconnecting, setIsReconnecting] = useState(false);

  const activeClients = clients.filter(c => c.online && !c.blocked);
  const wifi24Clients = clients.filter(c => c.online && c.connectionType === 'wifi24').length;
  const wifi5Clients = clients.filter(c => c.online && c.connectionType === 'wifi5').length;
  const ethClients = clients.filter(c => c.online && c.connectionType === 'eth').length;

  const handleReboot = async () => {
    setIsRebooting(true);
    setShowRebootConfirm(false);
    await onReboot();
    setTimeout(() => {
      setIsRebooting(false);
    }, 4000);
  };

  const handleReconnect = async () => {
    setIsReconnecting(true);
    await onReconnectWan();
    setTimeout(() => {
      setIsReconnecting(false);
    }, 2500);
  };

  return (
    <div className="space-y-6">
      {/* Reboot Alert Banner if active */}
      {isRebooting && (
        <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 flex items-center justify-between animate-pulse">
          <div className="flex items-center space-x-3">
            <RotateCcw className="w-5 h-5 animate-spin text-amber-400" />
            <div>
              <p className="font-semibold text-sm">Перезагрузка KeeneticOS в процессе...</p>
              <p className="text-xs text-amber-400/70">Пожалуйста, подождите. Службы маршрутизатора перезапускаются.</p>
            </div>
          </div>
        </div>
      )}

      {/* Top 4 Key Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* CPU Load Card */}
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] hover:border-zinc-700 transition">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
                <Cpu className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">ЦП Нагрузка</span>
            </div>
            <span className="text-xs font-medium px-2 py-0.5 rounded bg-[#27272a] text-zinc-300">
              {system?.cpuCores || 2} Ядра
            </span>
          </div>
          <div className="flex items-baseline justify-between mb-2">
            <span className="text-2xl font-bold text-[#fafafa]">{system?.cpuload ?? 12}%</span>
            <span className="text-xs text-zinc-400 truncate max-w-[140px]">{system?.cpuFreq || 'MT7621A'}</span>
          </div>
          {/* Progress bar */}
          <div className="w-full h-1.5 bg-[#27272a] rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                (system?.cpuload || 0) > 75
                  ? 'bg-rose-500'
                  : (system?.cpuload || 0) > 40
                  ? 'bg-amber-400'
                  : 'bg-blue-500'
              }`}
              style={{ width: `${Math.min(100, system?.cpuload || 12)}%` }}
            />
          </div>
        </div>

        {/* RAM Card */}
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] hover:border-zinc-700 transition">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-lg bg-purple-500/10 text-purple-400">
                <HardDrive className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Память (RAM)</span>
            </div>
            <span className="text-xs font-medium px-2 py-0.5 rounded bg-[#27272a] text-zinc-300">
              {system?.memPercent || 30}%
            </span>
          </div>
          <div className="flex items-baseline justify-between mb-2">
            <span className="text-2xl font-bold text-[#fafafa]">
              {system ? Math.round(system.memused / 1024) : 148} MB
            </span>
            <span className="text-xs text-zinc-400">
              из {system ? Math.round(system.memtotal / 1024) : 512} MB
            </span>
          </div>
          <div className="w-full h-1.5 bg-[#27272a] rounded-full overflow-hidden">
            <div
              className="h-full rounded-full bg-purple-500 transition-all duration-500"
              style={{ width: `${Math.min(100, system?.memPercent || 30)}%` }}
            />
          </div>
        </div>

        {/* WAN Internet Card */}
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] hover:border-zinc-700 transition">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
                <Globe className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Интернет (WAN)</span>
            </div>
            <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              Online
            </span>
          </div>
          <div className="flex items-baseline justify-between mb-1">
            <span className="text-lg font-bold text-[#fafafa] font-mono">{wan?.ip || '94.25.180.44'}</span>
          </div>
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span className="truncate max-w-[140px]">{wan?.isp || 'Провайдер'}</span>
            <span className="text-emerald-400 font-mono font-medium">{wan?.rxspeedMbps || 0} Мбит/с</span>
          </div>
        </div>

        {/* Clients Count Card */}
        <div
          onClick={() => onNavigateToTab('clients')}
          className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] hover:border-zinc-600 transition cursor-pointer group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400 group-hover:bg-blue-500/20 transition">
                <Server className="w-4 h-4" />
              </div>
              <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400">Клиенты</span>
            </div>
            <span className="text-xs font-medium text-blue-400 group-hover:underline">
              Все {clients.length} →
            </span>
          </div>
          <div className="flex items-baseline justify-between mb-2">
            <span className="text-2xl font-bold text-[#fafafa]">{activeClients.length}</span>
            <span className="text-xs text-emerald-400 font-medium">Активны</span>
          </div>
          <div className="flex items-center space-x-3 text-xs text-zinc-400">
            <span>5G: <strong className="text-zinc-200">{wifi5Clients}</strong></span>
            <span>2.4G: <strong className="text-zinc-200">{wifi24Clients}</strong></span>
            <span>LAN: <strong className="text-zinc-200">{ethClients}</strong></span>
          </div>
        </div>
      </div>

      {/* Real-time Bandwidth & Traffic Speed Chart */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
          <div>
            <h2 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
              <Zap className="w-4 h-4 text-blue-400" />
              Скорость трафика в реальном времени (WAN)
            </h2>
            <p className="text-xs text-zinc-400">Мгновенная скорость приема и передачи данных</p>
          </div>

          <div className="flex items-center space-x-4 text-xs">
            <div className="flex items-center space-x-1.5">
              <div className="w-2.5 h-2.5 rounded-full bg-[#3b82f6]" />
              <span className="text-zinc-400 font-medium">Загрузка:</span>
              <span className="font-mono font-bold text-blue-400">{wan?.rxspeedMbps || 0} Мбит/с</span>
            </div>
            <div className="flex items-center space-x-1.5">
              <div className="w-2.5 h-2.5 rounded-full bg-[#10b981]" />
              <span className="text-zinc-400 font-medium">Отдача:</span>
              <span className="font-mono font-bold text-emerald-400">{wan?.txspeedMbps || 0} Мбит/с</span>
            </div>
          </div>
        </div>

        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={trafficHistory} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="downloadGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.35} />
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                </linearGradient>
                <linearGradient id="uploadGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.35} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#27272a" opacity={0.8} />
              <XAxis dataKey="time" stroke="#71717a" tick={{ fontSize: 11 }} />
              <YAxis stroke="#71717a" tick={{ fontSize: 11 }} unit="M" />
              <Tooltip
                contentStyle={{
                  backgroundColor: '#18181b',
                  borderColor: '#27272a',
                  borderRadius: '0.5rem',
                  color: '#fafafa',
                  fontSize: '12px',
                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.5)',
                }}
              />
              <Area
                type="monotone"
                dataKey="downloadMbps"
                name="Загрузка (Мбит/с)"
                stroke="#3b82f6"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#downloadGrad)"
                isAnimationActive={false}
              />
              <Area
                type="monotone"
                dataKey="uploadMbps"
                name="Отдача (Мбит/с)"
                stroke="#10b981"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#uploadGrad)"
                isAnimationActive={false}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Hardware Ports & Quick Actions Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Hardware Switch Panel Visualizer (2 cols) */}
        <div className="lg:col-span-2 p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Radio className="w-4 h-4 text-blue-400" />
              <h2 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider">
                Состояние портов коммутатора (RJ-45)
              </h2>
            </div>
            <button
              onClick={() => onNavigateToTab('ports')}
              className="text-xs text-blue-400 hover:text-blue-300 font-medium cursor-pointer"
            >
              Подробнее →
            </button>
          </div>

          {/* Physical Switch Layout Illustration */}
          <div className="p-4 rounded-xl bg-[#09090b] border border-[#27272a] flex flex-wrap items-center justify-around gap-3">
            {ports.map(port => {
              const isWan = port.type === 'WAN';
              const isConnected = port.link;

              return (
                <div
                  key={port.portNumber}
                  className={`flex flex-col items-center p-3 rounded-lg border min-w-[110px] transition ${
                    isConnected
                      ? isWan
                        ? 'bg-blue-950/20 border-blue-500/40'
                        : 'bg-[#18181b] border-[#27272a]'
                      : 'bg-[#09090b] border-[#27272a]/50 opacity-50'
                  }`}
                >
                  {/* Port Port Jack Icon & LED */}
                  <div className="relative mb-2">
                    <div
                      className={`w-12 h-10 rounded border flex items-center justify-center font-mono text-[10px] font-bold ${
                        isConnected
                          ? isWan
                            ? 'border-blue-500 bg-blue-950/40 text-blue-300'
                            : 'border-emerald-500 bg-emerald-950/40 text-emerald-300'
                          : 'border-[#27272a] bg-[#18181b] text-zinc-600'
                      }`}
                    >
                      {port.label.split(' ')[0]}
                    </div>
                    {/* Link LED */}
                    <div
                      className={`absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full border border-[#09090b] ${
                        isConnected
                          ? isWan
                            ? 'bg-blue-400 animate-pulse'
                            : 'bg-emerald-400'
                          : 'bg-zinc-700'
                      }`}
                    />
                  </div>

                  <span className="text-xs font-semibold text-[#fafafa]">{port.label}</span>
                  <span className={`text-[10px] font-mono font-medium ${isConnected ? 'text-emerald-400' : 'text-zinc-500'}`}>
                    {isConnected ? port.speed : 'Отключен'}
                  </span>
                  {port.connectedDevice && (
                    <span className="text-[10px] text-zinc-400 truncate max-w-[100px] mt-0.5" title={port.connectedDevice}>
                      {port.connectedDevice}
                    </span>
                  )}
                </div>
              );
            })}
          </div>

          {/* Wi-Fi Quick Access Summary */}
          <div className="mt-4 pt-4 border-t border-[#27272a] grid grid-cols-1 sm:grid-cols-2 gap-3">
            {wifiList.map(wifi => (
              <div
                key={wifi.id}
                className="flex items-center justify-between p-3 rounded-lg bg-[#09090b] border border-[#27272a]"
              >
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg ${wifi.enabled ? 'bg-blue-500/10 text-blue-400' : 'bg-[#18181b] text-zinc-600'}`}>
                    <Wifi className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="text-xs font-semibold text-[#fafafa]">{wifi.ssid}</span>
                      <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#18181b] text-zinc-300 font-mono border border-[#27272a]">
                        {wifi.band}
                      </span>
                    </div>
                    <p className="text-[11px] text-zinc-400">
                      Канал: <span className="font-mono text-zinc-300">{wifi.actualChannel}</span> • {wifi.clientsCount} устр.
                    </p>
                  </div>
                </div>

                {/* Toggle Switch */}
                <button
                  id={`quick-toggle-wifi-${wifi.id}`}
                  onClick={() => onToggleWifi(wifi.id, !wifi.enabled)}
                  className={`w-9 h-5 rounded-full transition p-0.5 cursor-pointer ${
                    wifi.enabled ? 'bg-blue-600' : 'bg-zinc-700'
                  }`}
                  title={wifi.enabled ? 'Отключить точку' : 'Включить точку'}
                >
                  <div
                    className={`w-4 h-4 rounded-full bg-white transition transform ${
                      wifi.enabled ? 'translate-x-4' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Router Operations (1 col) */}
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider mb-4 flex items-center gap-2">
              <Power className="w-4 h-4 text-blue-400" />
              Быстрое управление Keenetic
            </h2>

            <div className="space-y-2.5">
              {/* WAN Reconnect Button */}
              <button
                id="wan-reconnect-btn"
                onClick={handleReconnect}
                disabled={isReconnecting}
                className="w-full flex items-center justify-between p-3 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-200 border border-[#27272a] transition cursor-pointer text-xs font-medium"
              >
                <div className="flex items-center space-x-2.5">
                  <RotateCcw className={`w-4 h-4 text-blue-400 ${isReconnecting ? 'animate-spin' : ''}`} />
                  <span>Переподключить WAN (DHCP)</span>
                </div>
                <span className="text-[11px] text-zinc-500">Сброс</span>
              </button>

              {/* Ping Test Shortcut */}
              <button
                id="quick-ping-btn"
                onClick={() => onNavigateToTab('diagnostics')}
                className="w-full flex items-center justify-between p-3 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-200 border border-[#27272a] transition cursor-pointer text-xs font-medium"
              >
                <div className="flex items-center space-x-2.5">
                  <Globe className="w-4 h-4 text-emerald-400" />
                  <span>Проверить Ping & Пакеты</span>
                </div>
                <span className="text-[11px] text-emerald-400">0% потерь</span>
              </button>

              {/* AI Diagnostics Shortcut */}
              <button
                id="quick-ai-diagnose-btn"
                onClick={() => onNavigateToTab('ai')}
                className="w-full flex items-center justify-between p-3 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-200 border border-[#27272a] transition cursor-pointer text-xs font-medium"
              >
                <div className="flex items-center space-x-2.5">
                  <ShieldCheck className="w-4 h-4 text-purple-400" />
                  <span>AI Аудит безопасности и Wi-Fi</span>
                </div>
                <span className="text-[11px] text-purple-400">Аудит</span>
              </button>

              {/* Reboot Router Button */}
              <button
                id="router-reboot-btn"
                onClick={() => setShowRebootConfirm(true)}
                className="w-full flex items-center justify-between p-3 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/20 transition cursor-pointer text-xs font-medium"
              >
                <div className="flex items-center space-x-2.5">
                  <Power className="w-4 h-4 text-rose-400" />
                  <span>Перезагрузить роутер</span>
                </div>
                <span className="text-[11px] text-rose-400">Reboot</span>
              </button>
            </div>
          </div>

          {/* System Info Footnote */}
          <div className="mt-6 pt-4 border-t border-[#27272a] text-xs text-zinc-400 space-y-1 font-mono">
            <div className="flex justify-between">
              <span className="text-zinc-500">NDM Version:</span>
              <span className="text-zinc-300">{system?.ndmVersion || '4.2.3'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-zinc-500">Хостнейм:</span>
              <span className="text-zinc-300">{system?.hostname || 'Keenetic'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-zinc-500">Всего принято:</span>
              <span className="text-zinc-300">{formatBytes(wan?.rxbytes || 0)}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Reboot Confirm Modal */}
      {showRebootConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-[#18181b] border border-[#27272a] rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-rose-400">
              <AlertCircle className="w-6 h-6" />
              <h3 className="text-lg font-bold text-[#fafafa]">Перезагрузка роутера</h3>
            </div>
            <p className="text-sm text-zinc-300 leading-relaxed">
              Вы уверены, что хотите перезагрузить маршрутизатор Keenetic? Все активные сетевые соединения будут временно разорваны на 30–60 секунд.
            </p>
            <div className="flex justify-end space-x-3 pt-2">
              <button
                onClick={() => setShowRebootConfirm(false)}
                className="px-4 py-2 rounded-lg bg-[#27272a] text-zinc-300 hover:bg-[#3f3f46] text-sm font-medium transition cursor-pointer"
              >
                Отмена
              </button>
              <button
                onClick={handleReboot}
                className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white text-sm font-semibold transition cursor-pointer flex items-center space-x-1.5"
              >
                <Power className="w-4 h-4" />
                <span>Перезагрузить</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
