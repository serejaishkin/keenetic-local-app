import React, { useState } from 'react';
import {
  Laptop,
  Smartphone,
  Tv,
  Server,
  Gamepad2,
  Tablet,
  Home,
  ShieldAlert,
  ShieldCheck,
  Search,
  SlidersHorizontal,
  Edit2,
  Gauge,
  Wifi,
  Radio,
  ArrowDown,
  ArrowUp,
  Ban,
  CheckCircle2,
  X,
  Save,
  HardDrive
} from 'lucide-react';
import { ClientDevice } from '../types';
import { formatBytes, formatSpeed } from '../services/routerService';

interface ClientsTabProps {
  clients: ClientDevice[];
  onToggleBlock: (mac: string, blocked: boolean) => void;
  onSetSpeedLimit: (mac: string, limitKbps: number) => void;
  onUpdateClient: (mac: string, name: string, dhcpStatic: boolean, ip?: string) => void;
}

export const ClientsTab: React.FC<ClientsTabProps> = ({
  clients,
  onToggleBlock,
  onSetSpeedLimit,
  onUpdateClient,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'online' | 'wifi5' | 'wifi24' | 'eth' | 'blocked'>('all');
  const [editingClient, setEditingClient] = useState<ClientDevice | null>(null);
  const [editName, setEditName] = useState('');
  const [editStatic, setEditStatic] = useState(false);
  const [editIp, setEditIp] = useState('');
  const [editSpeedLimit, setEditSpeedLimit] = useState(0);

  // Filter clients
  const filteredClients = clients.filter(client => {
    // Search query match
    const query = searchQuery.toLowerCase();
    const matchesSearch =
      client.hostname.toLowerCase().includes(query) ||
      (client.customName && client.customName.toLowerCase().includes(query)) ||
      client.ip.toLowerCase().includes(query) ||
      client.mac.toLowerCase().includes(query) ||
      client.vendor.toLowerCase().includes(query);

    if (!matchesSearch) return false;

    // Filter type match
    if (filterType === 'online') return client.online && !client.blocked;
    if (filterType === 'wifi5') return client.connectionType === 'wifi5';
    if (filterType === 'wifi24') return client.connectionType === 'wifi24';
    if (filterType === 'eth') return client.connectionType === 'eth';
    if (filterType === 'blocked') return client.blocked;

    return true;
  });

  const getDeviceIcon = (iconType: string, connType: string) => {
    switch (iconType) {
      case 'laptop':
        return <Laptop className="w-5 h-5 text-cyan-400" />;
      case 'phone':
        return <Smartphone className="w-5 h-5 text-indigo-400" />;
      case 'tv':
        return <Tv className="w-5 h-5 text-purple-400" />;
      case 'server':
        return <Server className="w-5 h-5 text-emerald-400" />;
      case 'console':
        return <Gamepad2 className="w-5 h-5 text-rose-400" />;
      case 'tablet':
        return <Tablet className="w-5 h-5 text-amber-400" />;
      case 'smart_home':
        return <Home className="w-5 h-5 text-teal-400" />;
      default:
        return connType === 'eth' ? <Server className="w-5 h-5 text-slate-400" /> : <Wifi className="w-5 h-5 text-cyan-400" />;
    }
  };

  const getRssiBars = (rssi?: number) => {
    if (!rssi) return null;
    let bars = 1;
    let color = 'bg-rose-400';
    if (rssi > -60) {
      bars = 4;
      color = 'bg-emerald-400';
    } else if (rssi > -70) {
      bars = 3;
      color = 'bg-cyan-400';
    } else if (rssi > -80) {
      bars = 2;
      color = 'bg-amber-400';
    }

    return (
      <div className="flex items-end space-x-0.5 h-3.5" title={`Уровень сигнала: ${rssi} dBm`}>
        <div className={`w-1 rounded-xs h-1 ${bars >= 1 ? color : 'bg-slate-700'}`} />
        <div className={`w-1 rounded-xs h-2 ${bars >= 2 ? color : 'bg-slate-700'}`} />
        <div className={`w-1 rounded-xs h-2.5 ${bars >= 3 ? color : 'bg-slate-700'}`} />
        <div className={`w-1 rounded-xs h-3.5 ${bars >= 4 ? color : 'bg-slate-700'}`} />
      </div>
    );
  };

  const openEditModal = (client: ClientDevice) => {
    setEditingClient(client);
    setEditName(client.customName || client.hostname);
    setEditStatic(client.dhcpStatic);
    setEditIp(client.ip);
    setEditSpeedLimit(client.speedLimitKbps);
  };

  const handleSaveClient = () => {
    if (!editingClient) return;
    onUpdateClient(editingClient.mac, editName, editStatic, editIp);
    if (editSpeedLimit !== editingClient.speedLimitKbps) {
      onSetSpeedLimit(editingClient.mac, editSpeedLimit);
    }
    setEditingClient(null);
  };

  return (
    <div className="space-y-6">
      {/* Overview Stats Top Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
          <span className="text-[11px] font-medium text-zinc-400 block">Всего устройств</span>
          <span className="text-xl font-bold text-[#fafafa] font-mono">{clients.length}</span>
        </div>
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
          <span className="text-[11px] font-medium text-zinc-400 block">В сети (Online)</span>
          <span className="text-xl font-bold text-emerald-400 font-mono">
            {clients.filter(c => c.online && !c.blocked).length}
          </span>
        </div>
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
          <span className="text-[11px] font-medium text-zinc-400 block">Wi-Fi 5 GHz</span>
          <span className="text-xl font-bold text-blue-400 font-mono">
            {clients.filter(c => c.online && c.connectionType === 'wifi5').length}
          </span>
        </div>
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
          <span className="text-[11px] font-medium text-zinc-400 block">Wi-Fi 2.4 GHz</span>
          <span className="text-xl font-bold text-purple-400 font-mono">
            {clients.filter(c => c.online && c.connectionType === 'wifi24').length}
          </span>
        </div>
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
          <span className="text-[11px] font-medium text-zinc-400 block">Заблокировано</span>
          <span className="text-xl font-bold text-rose-400 font-mono">
            {clients.filter(c => c.blocked).length}
          </span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between p-4 rounded-xl bg-[#18181b] border border-[#27272a]">
        {/* Search */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Поиск по имени, IP, MAC или вендору..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Filter Buttons */}
        <div className="flex flex-wrap gap-1.5 w-full sm:w-auto">
          {[
            { id: 'all', label: 'Все' },
            { id: 'online', label: 'Онлайн' },
            { id: 'wifi5', label: 'Wi-Fi 5G' },
            { id: 'wifi24', label: 'Wi-Fi 2.4G' },
            { id: 'eth', label: 'Проводные' },
            { id: 'blocked', label: 'Блокированные' },
          ].map(f => (
            <button
              key={f.id}
              onClick={() => setFilterType(f.id as any)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition cursor-pointer ${
                filterType === f.id
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'bg-[#09090b] hover:bg-[#27272a] text-zinc-300 border border-[#27272a]'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Clients List */}
      <div className="space-y-3">
        {filteredClients.length === 0 ? (
          <div className="p-8 text-center rounded-xl bg-[#18181b] border border-[#27272a] text-zinc-400 text-sm">
            Устройства по вашему запросу не найдены.
          </div>
        ) : (
          filteredClients.map(client => {
            const displayName = client.customName || client.hostname;
            const isOnline = client.online && !client.blocked;

            return (
              <div
                key={client.mac}
                className={`p-4 rounded-xl border transition flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                  client.blocked
                    ? 'bg-rose-950/15 border-rose-900/30'
                    : isOnline
                    ? 'bg-[#18181b] border-[#27272a] hover:border-zinc-700'
                    : 'bg-[#09090b] border-[#27272a]/60 opacity-60'
                }`}
              >
                {/* Left: Icon & Device Info */}
                <div className="flex items-start space-x-3.5 min-w-[280px]">
                  <div
                    className={`p-3 rounded-lg ${
                      client.blocked
                        ? 'bg-rose-500/10 text-rose-400'
                        : isOnline
                        ? 'bg-[#27272a]'
                        : 'bg-[#18181b] text-zinc-600'
                    }`}
                  >
                    {getDeviceIcon(client.iconType, client.connectionType)}
                  </div>

                  <div className="space-y-0.5">
                    <div className="flex items-center space-x-2">
                      <h4 className="text-sm font-semibold text-[#fafafa] tracking-tight">{displayName}</h4>
                      {client.dhcpStatic && (
                        <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-purple-500/15 text-purple-300 border border-purple-500/20">
                          Static IP
                        </span>
                      )}
                      {client.speedLimitKbps > 0 && (
                        <span className="text-[10px] font-semibold px-1.5 py-0.2 rounded bg-amber-500/15 text-amber-300 border border-amber-500/20 flex items-center gap-0.5">
                          <Gauge className="w-2.5 h-2.5" />
                          {(client.speedLimitKbps / 1024).toFixed(0)} Мбит/с
                        </span>
                      )}
                    </div>

                    <div className="flex items-center space-x-2 text-xs text-zinc-400">
                      <span className="font-mono text-zinc-300">{client.ip}</span>
                      <span>•</span>
                      <span className="font-mono text-zinc-500">{client.mac}</span>
                    </div>

                    <div className="text-[11px] text-zinc-400">
                      <span>{client.vendor}</span>
                      {client.hostname !== displayName && (
                        <span className="text-zinc-500 font-mono ml-1.5">({client.hostname})</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Center: Connection Type & Real-time Speed */}
                <div className="flex flex-wrap items-center gap-4 text-xs">
                  {/* Connection badge */}
                  <div className="flex items-center space-x-2 bg-[#09090b] px-3 py-1.5 rounded-lg border border-[#27272a]">
                    {client.connectionType === 'wifi5' ? (
                      <>
                        <Wifi className="w-3.5 h-3.5 text-blue-400" />
                        <span className="font-medium text-blue-300">5 GHz (AX)</span>
                        {getRssiBars(client.rssi)}
                      </>
                    ) : client.connectionType === 'wifi24' ? (
                      <>
                        <Wifi className="w-3.5 h-3.5 text-purple-400" />
                        <span className="font-medium text-purple-300">2.4 GHz</span>
                        {getRssiBars(client.rssi)}
                      </>
                    ) : (
                      <>
                        <Server className="w-3.5 h-3.5 text-emerald-400" />
                        <span className="font-medium text-emerald-300">LAN Port {client.port || '1'} (1G)</span>
                      </>
                    )}
                  </div>

                  {/* Real-time Speeds */}
                  {isOnline && (
                    <div className="flex items-center space-x-3 font-mono">
                      <div className="flex items-center space-x-1 text-blue-400">
                        <ArrowDown className="w-3.5 h-3.5" />
                        <span>{formatSpeed(client.rxSpeed)}</span>
                      </div>
                      <div className="flex items-center space-x-1 text-emerald-400">
                        <ArrowUp className="w-3.5 h-3.5" />
                        <span>{formatSpeed(client.txSpeed)}</span>
                      </div>
                    </div>
                  )}

                  {/* Total transferred */}
                  <div className="text-[11px] text-zinc-400 hidden lg:block">
                    Всего: <span className="font-mono text-zinc-200">{formatBytes(client.rxBytes + client.txBytes)}</span>
                  </div>
                </div>

                {/* Right: Actions */}
                <div className="flex items-center space-x-2 self-end md:self-center">
                  <button
                    onClick={() => openEditModal(client)}
                    className="p-2 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] transition cursor-pointer"
                    title="Редактировать имя, IP и лимит скорости"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>

                  <button
                    onClick={() => onToggleBlock(client.mac, !client.blocked)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition cursor-pointer ${
                      client.blocked
                        ? 'bg-rose-600 hover:bg-rose-500 text-white shadow'
                        : 'bg-[#09090b] hover:bg-rose-950/30 text-zinc-300 hover:text-rose-400 border border-[#27272a]'
                    }`}
                  >
                    {client.blocked ? (
                      <>
                        <ShieldAlert className="w-3.5 h-3.5 text-white" />
                        <span>Разблокировать</span>
                      </>
                    ) : (
                      <>
                        <Ban className="w-3.5 h-3.5 text-zinc-400 hover:text-rose-400" />
                        <span>Блокировать</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Edit Client Modal */}
      {editingClient && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-[#18181b] border border-[#27272a] rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-bold text-[#fafafa] flex items-center gap-2">
                <Edit2 className="w-4 h-4 text-blue-400" />
                Настройки устройства
              </h3>
              <button
                onClick={() => setEditingClient(null)}
                className="text-zinc-400 hover:text-white p-1 rounded-lg cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3.5 text-xs">
              {/* Custom Name */}
              <div>
                <label className="block text-zinc-400 font-medium mb-1">Пользовательское имя устройства</label>
                <input
                  type="text"
                  value={editName}
                  onChange={e => setEditName(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-medium focus:outline-none focus:border-blue-500"
                />
              </div>

              {/* IP & Static Reservation */}
              <div>
                <label className="block text-zinc-400 font-medium mb-1">IP адрес (DHCP)</label>
                <input
                  type="text"
                  value={editIp}
                  onChange={e => setEditIp(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-lg bg-[#09090b] border border-[#27272a]">
                <div>
                  <span className="font-semibold text-white block">Постоянный IP (Static DHCP)</span>
                  <span className="text-[11px] text-zinc-400">Закрепляет текущий IP за MAC-адресом</span>
                </div>
                <input
                  type="checkbox"
                  checked={editStatic}
                  onChange={e => setEditStatic(e.target.checked)}
                  className="w-4 h-4 rounded text-blue-500 focus:ring-0 cursor-pointer"
                />
              </div>

              {/* Speed Limit */}
              <div>
                <label className="block text-zinc-400 font-medium mb-1">
                  Ограничение скорости (QoS Bandwidth Limit)
                </label>
                <div className="grid grid-cols-4 gap-2 mb-2">
                  {[
                    { label: 'Без лимита', kbps: 0 },
                    { label: '5 Мбит', kbps: 5120 },
                    { label: '10 Мбит', kbps: 10240 },
                    { label: '25 Мбит', kbps: 25600 },
                  ].map(opt => (
                    <button
                      key={opt.kbps}
                      type="button"
                      onClick={() => setEditSpeedLimit(opt.kbps)}
                      className={`py-1.5 px-2 rounded text-[11px] font-semibold border cursor-pointer ${
                        editSpeedLimit === opt.kbps
                          ? 'bg-blue-500/20 border-blue-500 text-blue-300'
                          : 'bg-[#09090b] border-[#27272a] text-zinc-400'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* MAC Footnote */}
              <div className="p-2.5 rounded-lg bg-[#09090b] border border-[#27272a] font-mono text-[11px] text-zinc-400 space-y-0.5">
                <div>MAC: <span className="text-zinc-200">{editingClient.mac}</span></div>
                <div>Хостнейм: <span className="text-zinc-200">{editingClient.hostname}</span></div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end space-x-2 pt-2">
              <button
                onClick={() => setEditingClient(null)}
                className="px-4 py-2 rounded-lg bg-[#27272a] text-zinc-300 hover:bg-[#3f3f46] text-xs font-semibold cursor-pointer"
              >
                Отмена
              </button>
              <button
                onClick={handleSaveClient}
                className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer"
              >
                <Save className="w-3.5 h-3.5" />
                <span>Сохранить в роутер</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
