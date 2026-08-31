import React from 'react';
import {
  LayoutDashboard,
  Wifi,
  Users,
  Layers,
  Network,
  ShieldCheck,
  Lock,
  Zap,
  FileText,
  Sparkles,
  ChevronRight,
  HardDrive
} from 'lucide-react';
import { ClientDevice, VpnConnection } from '../types';

interface SidebarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  clients: ClientDevice[];
  vpnList: VpnConnection[];
  isMobileOpen: boolean;
  setIsMobileOpen: (open: boolean) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  setActiveTab,
  clients,
  vpnList,
  isMobileOpen,
  setIsMobileOpen,
}) => {
  const onlineClientsCount = clients.filter(c => c.online && !c.blocked).length;
  const activeVpnCount = vpnList.filter(v => v.status === 'connected').length;

  const menuSections = [
    {
      title: 'Главное',
      items: [
        {
          id: 'dashboard',
          label: 'Системный монитор',
          icon: LayoutDashboard,
        },
      ],
    },
    {
      title: 'Мои сети и Wi-Fi',
      items: [
        {
          id: 'wifi',
          label: 'Домашняя сеть и Wi-Fi',
          icon: Wifi,
        },
        {
          id: 'clients',
          label: 'Список устройств',
          icon: Users,
          badge: `${onlineClientsCount}`,
          badgeColor: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30',
        },
        {
          id: 'ports',
          label: 'Сетевые порты',
          icon: Layers,
        },
      ],
    },
    {
      title: 'Сетевые правила',
      items: [
        {
          id: 'nat',
          label: 'Переадресация портов',
          icon: Network,
        },
        {
          id: 'dns',
          label: 'DNS-фильтрация',
          icon: ShieldCheck,
        },
        {
          id: 'vpn',
          label: 'VPN-соединения',
          icon: Lock,
          badge: activeVpnCount > 0 ? `${activeVpnCount} акт.` : undefined,
          badgeColor: 'bg-blue-500/15 text-blue-400 border-blue-500/30',
        },
      ],
    },
    {
      title: 'Управление и сервис',
      items: [
        {
          id: 'diagnostics',
          label: 'Диагностика и RCI',
          icon: Zap,
        },
        {
          id: 'syslog',
          label: 'Журнал событий',
          icon: FileText,
        },
        {
          id: 'ai',
          label: 'Keenetic AI Copilot',
          icon: Sparkles,
          badge: 'AI',
          badgeColor: 'bg-purple-500/15 text-purple-300 border-purple-500/30',
        },
      ],
    },
  ];

  const handleSelectTab = (tabId: string) => {
    setActiveTab(tabId);
    if (isMobileOpen) {
      setIsMobileOpen(false);
    }
  };

  return (
    <>
      {/* Mobile Backdrop */}
      {isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          className="fixed inset-0 bg-black/70 backdrop-blur-xs z-40 lg:hidden"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`fixed lg:static top-0 left-0 bottom-0 z-40 w-64 bg-[#18181b] border-r border-[#27272a] flex flex-col transition-transform duration-200 ease-in-out lg:translate-x-0 ${
          isMobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* KeeneticOS Brand Header */}
        <div className="h-16 flex items-center px-5 border-b border-[#27272a] gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-cyan-500 flex items-center justify-center text-white font-black text-base shadow-sm">
            K
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold text-[#fafafa] tracking-tight">KeeneticOS</span>
            <span className="text-[11px] font-mono text-zinc-400">Панель управления</span>
          </div>
        </div>

        {/* Navigation Groups */}
        <div className="flex-1 overflow-y-auto py-4 px-3 space-y-6 text-xs">
          {menuSections.map((section, idx) => (
            <div key={idx} className="space-y-1">
              <div className="px-3 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-zinc-500">
                {section.title}
              </div>
              <div className="space-y-0.5">
                {section.items.map(item => {
                  const Icon = item.icon;
                  const isActive = activeTab === item.id;
                  return (
                    <button
                      key={item.id}
                      onClick={() => handleSelectTab(item.id)}
                      className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg font-medium transition cursor-pointer text-left ${
                        isActive
                          ? 'bg-blue-600/15 text-blue-400 font-semibold border border-blue-500/30'
                          : 'text-zinc-300 hover:text-white hover:bg-[#27272a]/60 border border-transparent'
                      }`}
                    >
                      <div className="flex items-center space-x-2.5 min-w-0">
                        <Icon
                          className={`w-4 h-4 shrink-0 ${
                            isActive ? 'text-blue-400' : 'text-zinc-400'
                          }`}
                        />
                        <span className="truncate">{item.label}</span>
                      </div>
                      <div className="flex items-center space-x-1.5 shrink-0 ml-2">
                        {item.badge && (
                          <span
                            className={`px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold border ${item.badgeColor}`}
                          >
                            {item.badge}
                          </span>
                        )}
                        {isActive && <ChevronRight className="w-3.5 h-3.5 text-blue-400" />}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* Sidebar Footer Info */}
        <div className="p-3 border-t border-[#27272a] bg-[#09090b]/50">
          <div className="p-2.5 rounded-lg bg-[#09090b] border border-[#27272a] flex items-center justify-between text-[11px]">
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span className="font-semibold text-zinc-300">RCI v4.x</span>
            </div>
            <span className="text-zinc-400 font-mono">192.168.1.1</span>
          </div>
        </div>
      </aside>
    </>
  );
};
