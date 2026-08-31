import React, { useState } from 'react';
import {
  Network,
  Plus,
  Trash2,
  Check,
  X,
  Server,
  Shield,
  ArrowRight,
  Sparkles,
  Laptop
} from 'lucide-react';
import { PortForwardingRule, ClientDevice } from '../types';

interface PortForwardingTabProps {
  rules: PortForwardingRule[];
  clients: ClientDevice[];
  onToggleRule: (id: string, enabled: boolean) => Promise<void>;
  onAddRule: (rule: Omit<PortForwardingRule, 'id'>) => Promise<void>;
  onDeleteRule: (id: string) => Promise<void>;
}

export const PortForwardingTab: React.FC<PortForwardingTabProps> = ({
  rules,
  clients,
  onToggleRule,
  onAddRule,
  onDeleteRule,
}) => {
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);
  const [name, setName] = useState<string>('');
  const [protocol, setProtocol] = useState<'TCP' | 'UDP' | 'TCP/UDP'>('TCP');
  const [externalPort, setExternalPort] = useState<string>('');
  const [internalIp, setInternalIp] = useState<string>(clients[0]?.ip || '192.168.1.120');
  const [internalPort, setInternalPort] = useState<string>('');
  const [comment, setComment] = useState<string>('');

  const presets = [
    { name: 'Web Server (HTTP)', proto: 'TCP' as const, ext: '80', int: '80' },
    { name: 'Secure Web (HTTPS)', proto: 'TCP' as const, ext: '443', int: '443' },
    { name: 'SSH Remote Console', proto: 'TCP' as const, ext: '2222', int: '22' },
    { name: 'Minecraft Server', proto: 'TCP' as const, ext: '25565', int: '25565' },
    { name: 'Plex Media Server', proto: 'TCP' as const, ext: '32400', int: '32400' },
    { name: 'WireGuard Server', proto: 'UDP' as const, ext: '51820', int: '51820' },
    { name: 'Remote Desktop (RDP)', proto: 'TCP' as const, ext: '3389', int: '3389' },
    { name: 'Torrent (Transmission)', proto: 'TCP/UDP' as const, ext: '51413', int: '51413' },
  ];

  const handleApplyPreset = (preset: typeof presets[0]) => {
    setName(preset.name);
    setProtocol(preset.proto);
    setExternalPort(preset.ext);
    setInternalPort(preset.int);
  };

  const handleSaveNewRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !externalPort || !internalIp || !internalPort) return;

    await onAddRule({
      name,
      protocol,
      externalPort,
      internalIp,
      internalPort,
      enabled: true,
      comment,
    });

    setIsAddModalOpen(false);
    setName('');
    setExternalPort('');
    setInternalPort('');
    setComment('');
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Network className="w-5 h-5 text-blue-400" />
            Переадресация портов (Port Forwarding / NAT)
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Настройка правил проброса внешних портов маршрутизатора на локальные серверы, NAS, умный дом и игровые консоли
          </p>
        </div>

        <button
          onClick={() => setIsAddModalOpen(true)}
          className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition flex items-center space-x-1.5 cursor-pointer shadow-sm self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Добавить правило</span>
        </button>
      </div>

      {/* Rules Table */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm overflow-hidden space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">
            Активные правила переадресации ({rules.length})
          </span>
          <span className="text-xs font-mono text-emerald-400 font-medium">
            UPnP / NAT-PMP: Включено
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#09090b] text-zinc-400 font-semibold border-b border-[#27272a]">
              <tr>
                <th className="py-3 px-4">Состояние</th>
                <th className="py-3 px-4">Название правила</th>
                <th className="py-3 px-4">Протокол</th>
                <th className="py-3 px-4">Внешний порт</th>
                <th className="py-3 px-4">Внутренний IP : Порт</th>
                <th className="py-3 px-4">Описание</th>
                <th className="py-3 px-4 text-right">Действие</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#27272a] font-mono">
              {rules.map(rule => (
                <tr key={rule.id} className="hover:bg-[#27272a]/30 transition">
                  <td className="py-3.5 px-4">
                    <button
                      onClick={() => onToggleRule(rule.id, !rule.enabled)}
                      className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors cursor-pointer ${
                        rule.enabled ? 'bg-blue-600' : 'bg-zinc-700'
                      }`}
                    >
                      <span
                        className={`inline-block h-3.5 w-3.5 transform rounded-full bg-white transition-transform ${
                          rule.enabled ? 'translate-x-4.5' : 'translate-x-1'
                        }`}
                      />
                    </button>
                  </td>
                  <td className="py-3.5 px-4 font-sans font-semibold text-[#fafafa]">
                    <div className="flex items-center space-x-2">
                      <Server className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                      <span>{rule.name}</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="px-2 py-0.5 rounded bg-[#09090b] border border-[#27272a] text-blue-300 font-semibold text-[11px]">
                      {rule.protocol}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-bold text-amber-400">
                    WAN :{rule.externalPort}
                  </td>
                  <td className="py-3.5 px-4 text-emerald-400">
                    <div className="flex items-center space-x-1.5 font-bold">
                      <span>{rule.internalIp}</span>
                      <ArrowRight className="w-3 h-3 text-zinc-500" />
                      <span>:{rule.internalPort}</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-4 font-sans text-zinc-400 text-xs">
                    {rule.comment || '—'}
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <button
                      onClick={() => onDeleteRule(rule.id)}
                      className="p-1.5 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-400 hover:text-rose-400 transition cursor-pointer"
                      title="Удалить правило"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Rule Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-[#18181b] border border-[#27272a] rounded-xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-[#27272a] pb-3">
              <h3 className="text-sm font-semibold text-white flex items-center gap-2">
                <Network className="w-4 h-4 text-blue-400" />
                Новое правило переадресации портов
              </h3>
              <button
                onClick={() => setIsAddModalOpen(false)}
                className="text-zinc-400 hover:text-white cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Presets Chips */}
            <div>
              <label className="block text-[11px] font-semibold text-zinc-400 mb-2 uppercase tracking-wider">
                Быстрые шаблоны служб:
              </label>
              <div className="flex flex-wrap gap-1.5">
                {presets.map((p, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleApplyPreset(p)}
                    className="px-2.5 py-1 rounded bg-[#09090b] border border-[#27272a] hover:border-blue-500/50 hover:bg-blue-950/20 text-zinc-300 hover:text-white text-[11px] transition cursor-pointer font-medium"
                  >
                    {p.name}
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleSaveNewRule} className="space-y-3.5 text-xs">
              <div>
                <label className="block text-zinc-300 font-medium mb-1">Название правила</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={e => setName(e.target.value)}
                  placeholder="Например: Synology Web, SSH Server"
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Протокол</label>
                  <select
                    value={protocol}
                    onChange={e => setProtocol(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none"
                  >
                    <option value="TCP">TCP</option>
                    <option value="UDP">UDP</option>
                    <option value="TCP/UDP">TCP/UDP (Оба)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Внешний порт (WAN)</label>
                  <input
                    type="text"
                    required
                    value={externalPort}
                    onChange={e => setExternalPort(e.target.value)}
                    placeholder="Например: 443 или 8080"
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Локальный хост (Цель)</label>
                  <select
                    value={internalIp}
                    onChange={e => setInternalIp(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono focus:outline-none"
                  >
                    {clients.map(c => (
                      <option key={c.mac} value={c.ip}>
                        {c.customName || c.hostname} ({c.ip})
                      </option>
                    ))}
                    <option value="192.168.1.120">192.168.1.120 (Synology NAS)</option>
                    <option value="192.168.1.1">192.168.1.1 (Keenetic Router)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-zinc-300 font-medium mb-1">Внутренний порт</label>
                  <input
                    type="text"
                    required
                    value={internalPort}
                    onChange={e => setInternalPort(e.target.value)}
                    placeholder="Например: 443 или 22"
                    className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-zinc-300 font-medium mb-1">Комментарий (необязательно)</label>
                <input
                  type="text"
                  value={comment}
                  onChange={e => setComment(e.target.value)}
                  placeholder="Назначение сервиса"
                  className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-white focus:outline-none focus:border-blue-500"
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
                  Сохранить правило
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
