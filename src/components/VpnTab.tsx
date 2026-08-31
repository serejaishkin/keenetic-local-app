import React, { useState } from 'react';
import {
  Lock,
  Plus,
  ArrowUpRight,
  ArrowDownLeft,
  Activity,
  CheckCircle2,
  XCircle,
  Clock,
  Shield,
  Layers,
  X,
  Radio,
  FileCode
} from 'lucide-react';
import { VpnConnection } from '../types';

interface VpnTabProps {
  vpnList: VpnConnection[];
  onToggleVpn: (id: string, status: 'connected' | 'disconnected') => Promise<void>;
  onAddVpn: (vpn: Omit<VpnConnection, 'id' | 'txBytes' | 'rxBytes' | 'rxSpeed' | 'txSpeed' | 'uptime'>) => Promise<void>;
}

export const VpnTab: React.FC<VpnTabProps> = ({
  vpnList,
  onToggleVpn,
  onAddVpn,
}) => {
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);
  const [name, setName] = useState<string>('');
  const [type, setType] = useState<VpnConnection['type']>('WireGuard');
  const [serverAddress, setServerAddress] = useState<string>('');
  const [clientIp, setClientIp] = useState<string>('10.8.0.2/32');
  const [policy, setPolicy] = useState<VpnConnection['policy']>('all');
  const [configText, setConfigText] = useState<string>('');

  const formatBytes = (bytes: number) => {
    if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
    if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / 1024).toFixed(0)} KB`;
  };

  const formatSpeed = (bytesPerSec: number) => {
    const mbps = (bytesPerSec * 8) / 1000000;
    return `${mbps.toFixed(1)} Мбит/с`;
  };

  const handleSaveVpn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !serverAddress) return;

    await onAddVpn({
      name,
      type,
      status: 'connected',
      serverAddress,
      clientIp,
      policy,
      devicesCount: 2,
    });

    setIsAddModalOpen(false);
    setName('');
    setServerAddress('');
    setConfigText('');
  };

  return (
    <div className="space-y-6">
      {/* Header Info Banner */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Lock className="w-5 h-5 text-blue-400" />
            VPN-клиенты и туннели (WireGuard, OpenVPN, SSTP)
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Безопасные зашифрованные соединения для обхода блокировок, удаленного доступа в офис и выборочной маршрутизации устройств
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition flex items-center space-x-1.5 cursor-pointer shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Добавить VPN-туннель</span>
        </button>
      </div>

      {/* VPN Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {vpnList.map(vpn => {
          const isConnected = vpn.status === 'connected';
          return (
            <div
              key={vpn.id}
              className={`p-5 rounded-xl border transition-all flex flex-col justify-between space-y-4 ${
                isConnected
                  ? 'bg-[#18181b] border-blue-500/40 shadow-sm'
                  : 'bg-[#18181b]/60 border-[#27272a] opacity-75'
              }`}
            >
              <div>
                {/* Card Header */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-2.5">
                    <span className="px-2 py-0.5 rounded text-[11px] font-mono font-bold bg-[#09090b] text-blue-300 border border-[#27272a]">
                      {vpn.type}
                    </span>
                    <span className="text-sm font-semibold text-white truncate max-w-[150px]">
                      {vpn.name}
                    </span>
                  </div>

                  <button
                    onClick={() => onToggleVpn(vpn.id, isConnected ? 'disconnected' : 'connected')}
                    className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors cursor-pointer ${
                      isConnected ? 'bg-blue-600' : 'bg-zinc-700'
                    }`}
                  >
                    <span
                      className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform ${
                        isConnected ? 'translate-x-4.5' : 'translate-x-1'
                      }`}
                    />
                  </button>
                </div>

                {/* Server & IP Details */}
                <div className="space-y-1.5 text-xs text-zinc-400 font-mono">
                  <div className="flex items-center justify-between">
                    <span>Сервер:</span>
                    <span className="text-zinc-200 font-semibold">{vpn.serverAddress}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>IP в туннеле:</span>
                    <span className="text-blue-300">{vpn.clientIp}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>Маршрутизация:</span>
                    <span className="text-zinc-300 font-sans">
                      {vpn.policy === 'all'
                        ? 'Весь трафик'
                        : vpn.policy === 'specific_domains'
                        ? 'По списку доменов'
                        : `${vpn.devicesCount} устройства`}
                    </span>
                  </div>
                </div>
              </div>

              {/* Traffic Metrics */}
              <div className="pt-3 border-t border-[#27272a] space-y-2 text-xs">
                <div className="grid grid-cols-2 gap-2 font-mono">
                  <div className="p-2 rounded-lg bg-[#09090b] border border-[#27272a] flex items-center space-x-1.5">
                    <ArrowDownLeft className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Входящий</span>
                      <span className="font-semibold text-zinc-200">{formatBytes(vpn.rxBytes)}</span>
                    </div>
                  </div>

                  <div className="p-2 rounded-lg bg-[#09090b] border border-[#27272a] flex items-center space-x-1.5">
                    <ArrowUpRight className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                    <div>
                      <span className="text-[10px] text-zinc-500 block">Исходящий</span>
                      <span className="font-semibold text-zinc-200">{formatBytes(vpn.txBytes)}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center justify-between text-[11px] text-zinc-400 pt-1">
                  <div className="flex items-center space-x-1">
                    <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-400 animate-pulse' : 'bg-zinc-600'}`} />
                    <span>{isConnected ? `Подключен (${vpn.uptime})` : 'Отключен'}</span>
                  </div>
                  {isConnected && (
                    <span className="font-mono font-semibold text-emerald-400">
                      {formatSpeed(vpn.rxSpeed)}
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add VPN Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-[#18181b] border border-[#27272a] rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#27272a] pb-3">
              <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                <Lock className="w-4 h-4 text-blue-400" />
                Новое VPN-подключение
              </h3>
              <button
                onClick={() => setIsAddModalOpen(false)}
                className="text-zinc-400 hover:text-white cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveVpn} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-zinc-300 font-medium mb-1">Название туннеля</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={e => setName(e.target.value)}
                  placeholder="Например: Frankfurt WireGuard, Office VPN"
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Тип VPN протокола</label>
                  <select
                    value={type}
                    onChange={e => setType(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none"
                  >
                    <option value="WireGuard">WireGuard (Быстрый)</option>
                    <option value="OpenVPN">OpenVPN</option>
                    <option value="SSTP">SSTP (KeenDNS)</option>
                    <option value="IPsec">IPsec / IKEv2</option>
                  </select>
                </div>

                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Адрес сервера (Host:Port)</label>
                  <input
                    type="text"
                    required
                    value={serverAddress}
                    onChange={e => setServerAddress(e.target.value)}
                    placeholder="vpn.example.com:51820"
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-zinc-300 font-medium mb-1">Политика маршрутизации</label>
                <select
                  value={policy}
                  onChange={e => setPolicy(e.target.value as any)}
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none"
                >
                  <option value="all">Весь трафик пускать через этот VPN</option>
                  <option value="specific_domains">Выборочно: только указанные домены / сайты</option>
                  <option value="selected_devices">Только для выбранных устройств (Smart TV, ПК)</option>
                </select>
              </div>

              <div>
                <label className="block text-zinc-300 font-medium mb-1">Конфигурация (.conf или .ovpn)</label>
                <textarea
                  rows={4}
                  value={configText}
                  onChange={e => setConfigText(e.target.value)}
                  placeholder="[Interface]&#10;PrivateKey = ...&#10;Address = 10.8.0.2/32&#10;[Peer]&#10;PublicKey = ..."
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono text-[11px] focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex items-center justify-end space-x-2 pt-3 border-t border-[#27272a]">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-4 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-300 hover:text-white cursor-pointer"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-semibold cursor-pointer shadow-sm"
                >
                  Создать и подключить
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
