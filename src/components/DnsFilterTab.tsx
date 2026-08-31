import React, { useState } from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  Globe,
  Lock,
  Zap,
  CheckCircle2,
  Sliders,
  Sparkles,
  BarChart3,
  Server
} from 'lucide-react';
import { DnsFilterConfig } from '../types';

interface DnsFilterTabProps {
  config: DnsFilterConfig;
  onUpdateConfig: (updates: Partial<DnsFilterConfig>) => Promise<void>;
}

export const DnsFilterTab: React.FC<DnsFilterTabProps> = ({
  config,
  onUpdateConfig,
}) => {
  const [selectedMode, setSelectedMode] = useState<DnsFilterConfig['mode']>(config.mode);
  const [dohEnabled, setDohEnabled] = useState<boolean>(config.dohEnabled);
  const [customPrimary, setCustomPrimary] = useState<string>(config.customDnsPrimary || '1.1.1.1');
  const [customSecondary, setCustomSecondary] = useState<string>(config.customDnsSecondary || '1.0.0.1');
  const [isSaved, setIsSaved] = useState<boolean>(false);

  const dnsProviders = [
    {
      id: 'adguard' as const,
      name: 'AdGuard DNS',
      badge: 'Рекомендуется',
      badgeColor: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
      description: 'Автоматическая блокировка баннерной рекламы, всплывающих окон, счетчиков слежки и фишинговых доменов на всех домашних устройствах.',
      servers: '94.140.14.14 / 94.140.15.15',
      dohUrl: 'https://dns.adguard-dns.com/dns-query',
    },
    {
      id: 'cloudflare' as const,
      name: 'Cloudflare 1.1.1.2 (Security)',
      badge: 'Быстрый',
      badgeColor: 'bg-blue-500/15 text-blue-400 border-blue-500/30',
      description: 'Сверхбыстрый Anycast DNS с автоматической блокировкой вредоносного ПО (Malware & Phishing Protection).',
      servers: '1.1.1.2 / 1.0.0.2',
      dohUrl: 'https://security.cloudflare-dns.com/dns-query',
    },
    {
      id: 'nextdns' as const,
      name: 'NextDNS Cloud',
      badge: 'Pro Настройки',
      badgeColor: 'bg-purple-500/15 text-purple-300 border-purple-500/30',
      description: 'Индивидуальные фильтры, защита от криптомайнеров, родительский контроль по расписанию и детальные логи запросов.',
      servers: '45.90.28.0 / 45.90.30.0',
      dohUrl: 'https://dns.nextdns.io',
    },
    {
      id: 'yandex_safe' as const,
      name: 'Яндекс.DNS (Безопасный)',
      badge: 'RU Регион',
      badgeColor: 'bg-amber-500/15 text-amber-400 border-amber-500/30',
      description: 'Блокировка мошеннических ресурсов, зараженных сайтов и ботнетов с минимальным пингом по РФ и СНГ.',
      servers: '77.88.8.88 / 77.88.8.2',
      dohUrl: 'https://safe.dns.yandex.net/dns-query',
    },
    {
      id: 'yandex_family' as const,
      name: 'Яндекс.DNS (Семейный)',
      badge: 'Дети и Семья',
      badgeColor: 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30',
      description: 'Полная блокировка опасных сайтов, а также материалов для взрослых и принудительный безопасный поиск.',
      servers: '77.88.8.7 / 77.88.8.3',
      dohUrl: 'https://family.dns.yandex.net/dns-query',
    },
    {
      id: 'custom' as const,
      name: 'Пользовательский DNS',
      badge: 'Вручную',
      badgeColor: 'bg-zinc-700 text-zinc-300 border-zinc-600',
      description: 'Использовать собственные upstream DNS-серверы (например, локальный Pi-hole, AdGuard Home или DNS провайдера).',
      servers: 'Настраивается пользователем',
      dohUrl: '',
    },
  ];

  const handleSave = async () => {
    const activeProvider = dnsProviders.find(p => p.id === selectedMode);
    await onUpdateConfig({
      mode: selectedMode,
      dohEnabled,
      customDnsPrimary: customPrimary,
      customDnsSecondary: customSecondary,
      dohServer: activeProvider?.dohUrl || '',
    });
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 2500);
  };

  return (
    <div className="space-y-6">
      {/* Header Info Banner */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-blue-400" />
            Интернет-безопасность и DNS-фильтрация (DoH / DoT)
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Защита всех устройств домашней сети от рекламы, слежки, фишинга и вредоносных сайтов на уровне Keenetic DNS-прокси
          </p>
        </div>

        <button
          onClick={handleSave}
          className="px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition flex items-center space-x-2 cursor-pointer shadow-sm self-start sm:self-auto"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>{isSaved ? 'Настройки сохранены!' : 'Применить профиль'}</span>
        </button>
      </div>

      {/* DNS Analytics Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
          <div className="flex items-center justify-between text-xs text-zinc-400 mb-1">
            <span>Всего DNS-запросов (24ч)</span>
            <Globe className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-white">
            {config.totalQueriesCount.toLocaleString()}
          </div>
          <span className="text-[11px] text-zinc-500 font-medium">Средняя задержка: 11 мс</span>
        </div>

        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
          <div className="flex items-center justify-between text-xs text-zinc-400 mb-1">
            <span>Заблокировано угроз и рекламы</span>
            <ShieldAlert className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-400">
            {config.blockedQueriesCount.toLocaleString()}
          </div>
          <span className="text-[11px] text-emerald-400/90 font-medium">
            ~13.1% нежелательного трафика отсечено
          </span>
        </div>

        <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm">
          <div className="flex items-center justify-between text-xs text-zinc-400 mb-1">
            <span>Шифрование DNS-over-HTTPS</span>
            <Lock className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-2xl font-bold font-mono text-purple-400">
            {dohEnabled ? 'TLS 1.3 Активен' : 'Отключено (UDP)'}
          </div>
          <span className="text-[11px] text-zinc-400 font-medium">
            Провайдер не видит посещаемые домены
          </span>
        </div>
      </div>

      {/* DoH Toggle Strip */}
      <div className="p-4 rounded-xl bg-[#18181b] border border-[#27272a] flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-9 h-9 rounded-lg bg-blue-600/15 border border-blue-500/30 flex items-center justify-center text-blue-400">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <span className="text-sm font-semibold text-white block">
              DNS-over-HTTPS (DoH) / DNS-over-TLS (DoT)
            </span>
            <span className="text-xs text-zinc-400">
              Шифрует все DNS-запросы от роутера до резолвера, защищая от перехвата и подмены ответов
            </span>
          </div>
        </div>

        <button
          onClick={() => setDohEnabled(!dohEnabled)}
          className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors cursor-pointer shrink-0 ml-4 ${
            dohEnabled ? 'bg-blue-600' : 'bg-zinc-700'
          }`}
        >
          <span
            className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
              dohEnabled ? 'translate-x-6' : 'translate-x-1'
            }`}
          />
        </button>
      </div>

      {/* DNS Provider Cards Grid */}
      <div className="space-y-3">
        <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider block">
          Выберите профиль DNS фильтрации для домашнего сегмента:
        </span>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {dnsProviders.map(provider => {
            const isSelected = selectedMode === provider.id;
            return (
              <div
                key={provider.id}
                onClick={() => setSelectedMode(provider.id)}
                className={`p-5 rounded-xl border transition-all cursor-pointer flex flex-col justify-between space-y-4 ${
                  isSelected
                    ? 'bg-blue-950/20 border-blue-500 shadow-sm'
                    : 'bg-[#18181b] border-[#27272a] hover:border-zinc-600'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-semibold text-white">{provider.name}</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-semibold border ${provider.badgeColor}`}>
                      {provider.badge}
                    </span>
                  </div>
                  <p className="text-xs text-zinc-400 leading-relaxed">
                    {provider.description}
                  </p>
                </div>

                <div className="pt-3 border-t border-[#27272a] flex items-center justify-between text-xs">
                  <span className="font-mono text-zinc-400 text-[11px] truncate max-w-[170px]">
                    {provider.servers}
                  </span>
                  <div className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                    isSelected ? 'border-blue-400 bg-blue-500' : 'border-zinc-600'
                  }`}>
                    {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Custom DNS Inputs if selected */}
      {selectedMode === 'custom' && (
        <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] space-y-4">
          <h3 className="text-sm font-semibold text-white flex items-center gap-2">
            <Sliders className="w-4 h-4 text-blue-400" />
            Настройка собственных DNS серверов
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-zinc-300 font-medium mb-1">Основной DNS (Primary)</label>
              <input
                type="text"
                value={customPrimary}
                onChange={e => setCustomPrimary(e.target.value)}
                placeholder="1.1.1.1 или 8.8.8.8"
                className="w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] font-mono text-white focus:outline-none focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-zinc-300 font-medium mb-1">Дополнительный DNS (Secondary)</label>
              <input
                type="text"
                value={customSecondary}
                onChange={e => setCustomSecondary(e.target.value)}
                placeholder="1.0.0.1 или 8.8.4.4"
                className="w-full px-3.5 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] font-mono text-white focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
