import React from 'react';
import {
  Radio,
  HardDrive,
  Globe,
  Server,
  Layers,
  ArrowDown,
  ArrowUp,
  Cpu,
  Zap,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { PortStatus, WanStatus } from '../types';

interface PortsTabProps {
  ports: PortStatus[];
  wan: WanStatus | null;
}

export const PortsTab: React.FC<PortsTabProps> = ({ ports, wan }) => {
  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Radio className="w-5 h-5 text-blue-400" />
            Порты коммутатора и физические интерфейсы
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Контроль состояния разъемов RJ-45, скорости соединений (100M/1000M/2.5G), дуплекса и сегментов сети
          </p>
        </div>

        <div className="flex items-center space-x-2 text-xs">
          <span className="px-3 py-1.5 rounded-md bg-[#09090b] border border-[#27272a] text-zinc-300 font-medium">
            Свитч: <strong className="text-emerald-400 font-semibold">MediaTek Gigabit Switch</strong>
          </span>
        </div>
      </div>

      {/* Interactive Hardware Panel Diagram */}
      <div className="p-6 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
        <div className="flex items-center justify-between mb-4 border-b border-[#27272a] pb-3">
          <span className="text-xs font-semibold text-zinc-400 tracking-wider uppercase">
            Задняя панель маршрутизатора Keenetic (Rear Hardware Panel)
          </span>
          <span className="text-xs font-mono text-blue-400 font-medium">KN-1811 Switch Module</span>
        </div>

        {/* Panel Case & Ports Container */}
        <div className="p-6 rounded-xl bg-[#09090b] border border-[#27272a] flex flex-wrap items-center justify-center sm:justify-start gap-5 shadow-inner">
          {ports.map(port => {
            const isWan = port.type === 'WAN';
            const isConnected = port.link;

            return (
              <div
                key={port.portNumber}
                className={`relative flex flex-col items-center p-4 rounded-xl border transition min-w-[130px] ${
                  isConnected
                    ? isWan
                      ? 'border-blue-500/50 bg-blue-950/20 shadow-sm'
                      : 'border-emerald-500/50 bg-emerald-950/20 shadow-sm'
                    : 'border-[#27272a] bg-[#18181b]/50 opacity-60'
                }`}
              >
                {/* Status LED Bar */}
                <div className="flex items-center space-x-1.5 mb-2">
                  <div
                    className={`w-2.5 h-2.5 rounded-full ${
                      isConnected
                        ? isWan
                          ? 'bg-blue-400 animate-pulse'
                          : 'bg-emerald-400'
                        : 'bg-zinc-700'
                    }`}
                  />
                  <span className="text-[10px] font-mono text-zinc-400 uppercase">
                    {isConnected ? 'LINK / ACT' : 'OFF'}
                  </span>
                </div>

                {/* RJ45 Socket Silhouette */}
                <div
                  className={`w-14 h-12 rounded-lg border-2 flex items-center justify-center font-mono font-bold text-xs ${
                    isConnected
                      ? isWan
                        ? 'border-blue-400 bg-[#09090b] text-blue-300'
                        : 'border-emerald-400 bg-[#09090b] text-emerald-300'
                      : 'border-zinc-700 bg-[#09090b] text-zinc-600'
                  }`}
                >
                  <div className="w-8 h-6 border-b-2 border-l border-r rounded-xs flex items-center justify-center text-[10px]">
                    {port.label.split(' ')[0]}
                  </div>
                </div>

                <div className="mt-2.5 text-center">
                  <span className="text-xs font-semibold text-[#fafafa] block">{port.label}</span>
                  <span className={`text-[11px] font-mono font-semibold ${isConnected ? 'text-emerald-400' : 'text-zinc-500'}`}>
                    {isConnected ? `${port.speed} ${port.duplex}` : 'Отключен'}
                  </span>
                </div>

                {port.connectedDevice && (
                  <div className="mt-1 px-1.5 py-0.5 rounded bg-[#18181b] border border-[#27272a] text-[10px] text-zinc-300 truncate max-w-[110px]">
                    {port.connectedDevice}
                  </div>
                )}
              </div>
            );
          })}

          {/* USB 3.0 & 2.0 Port mock representation */}
          <div className="flex flex-col items-center p-4 rounded-xl border border-[#27272a] bg-[#18181b]/50 min-w-[110px]">
            <div className="w-2.5 h-2.5 rounded-full bg-blue-400 mb-2" />
            <div className="w-12 h-6 rounded border border-blue-500/60 bg-blue-950/30 text-[9px] font-bold text-blue-300 flex items-center justify-center font-mono">
              USB 3.0
            </div>
            <span className="text-xs font-medium text-zinc-300 mt-2.5">USB Порт 1</span>
            <span className="text-[10px] text-blue-400 font-mono">Ready (Storage/Modem)</span>
          </div>
        </div>
      </div>

      {/* Detailed Ports Statistics Table */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm overflow-hidden">
        <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider mb-4 flex items-center gap-2">
          <Layers className="w-4 h-4 text-blue-400" />
          Сводная таблица портов и сетевых сегментов
        </h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#09090b] text-zinc-400 font-semibold border-b border-[#27272a]">
              <tr>
                <th className="py-3 px-4">Разъем</th>
                <th className="py-3 px-4">Тип</th>
                <th className="py-3 px-4">Статус Link</th>
                <th className="py-3 px-4">Скорость</th>
                <th className="py-3 px-4">Дуплекс</th>
                <th className="py-3 px-4">Подключенное устройство</th>
                <th className="py-3 px-4">Сегмент сети</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#27272a] font-mono">
              {ports.map(port => (
                <tr key={port.portNumber} className="hover:bg-[#27272a]/40 transition">
                  <td className="py-3 px-4 font-semibold text-[#fafafa] flex items-center gap-2">
                    <span className={`w-2 h-2 rounded-full ${port.link ? 'bg-emerald-400' : 'bg-zinc-600'}`} />
                    {port.label}
                  </td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${
                      port.type === 'WAN' ? 'bg-blue-500/15 text-blue-300 border border-blue-500/20' : 'bg-[#27272a] text-zinc-300 border border-[#27272a]'
                    }`}>
                      {port.type}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span className={port.link ? 'text-emerald-400 font-semibold' : 'text-zinc-500'}>
                      {port.link ? 'Up (Активен)' : 'Down (Нет кабеля)'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-zinc-200">{port.link ? port.speed : '—'}</td>
                  <td className="py-3 px-4 text-zinc-400">{port.duplex}</td>
                  <td className="py-3 px-4 text-zinc-300 font-sans">
                    {port.connectedDevice || <span className="text-zinc-600 font-mono">—</span>}
                  </td>
                  <td className="py-3 px-4 font-sans">
                    {port.type === 'WAN' ? (
                      <span className="text-blue-300">ISP Провайдер (Внешний)</span>
                    ) : (
                      <span className="text-zinc-400">Основной сегмент (Home 192.168.1.0/24)</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
