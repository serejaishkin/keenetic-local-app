import React from 'react';
import {
  Wifi,
  Shield,
  RefreshCw,
  Settings,
  Activity,
  Server,
  Zap,
  CheckCircle2,
  AlertTriangle,
  Radio
} from 'lucide-react';
import { RouterConfig, SystemStatus, WanStatus } from '../types';

interface HeaderProps {
  config: RouterConfig;
  system: SystemStatus | null;
  wan: WanStatus | null;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onOpenSettings: () => void;
  onRefresh: () => void;
  isRefreshing: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  config,
  system,
  wan,
  activeTab,
  setActiveTab,
  onOpenSettings,
  onRefresh,
  isRefreshing,
}) => {
  const tabs = [
    { id: 'dashboard', label: 'Обзор', icon: Activity },
    { id: 'wifi', label: 'Wi-Fi Сети', icon: Wifi },
    { id: 'clients', label: 'Устройства', icon: Server },
    { id: 'ports', label: 'Порты', icon: Radio },
    { id: 'diagnostics', label: 'Диагностика & RCI', icon: Zap },
    { id: 'ai', label: 'AI Copilot', icon: Shield },
  ];

  return (
    <header className="sticky top-0 z-30 bg-[#09090b]/95 backdrop-blur border-b border-[#27272a]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Top bar */}
        <div className="flex items-center justify-between h-16">
          {/* Logo & Model */}
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-[#3b82f6] rounded-md flex items-center justify-center font-bold text-white shadow-sm">
              K
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h1 className="text-base font-semibold text-[#fafafa] tracking-tight">Keenetic Local</h1>
                {config.isDemo ? (
                  <span className="px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">
                    Симулятор
                  </span>
                ) : (
                  <span className="px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    Online ({config.host})
                  </span>
                )}
              </div>
              <p className="text-xs text-zinc-400 font-medium">
                {system ? `${system.model} • KeeneticOS ${system.version}` : 'Keenetic Router Manager'}
              </p>
            </div>
          </div>

          {/* Center Info Stats (WAN / Uptime) on medium+ screens */}
          {system && (
            <div className="hidden lg:flex items-center space-x-4 text-xs text-zinc-300">
              <div className="flex items-center space-x-2 bg-[#18181b] px-3 py-1.5 rounded-md border border-[#27272a]">
                <div className="w-2 h-2 rounded-full bg-emerald-400" />
                <span className="text-zinc-500 uppercase text-[10px] font-medium tracking-wider">Local IP:</span>
                <span className="font-mono font-medium text-zinc-200">{config.host || '192.168.1.1'}</span>
              </div>
              <div className="flex items-center space-x-2 bg-[#18181b] px-3 py-1.5 rounded-md border border-[#27272a]">
                <span className="text-zinc-500 uppercase text-[10px] font-medium tracking-wider">Uptime:</span>
                <span className="font-mono font-medium text-zinc-200">{system.uptimeFormatted}</span>
              </div>
            </div>
          )}

          {/* Right Action buttons */}
          <div className="flex items-center space-x-2">
            <button
              id="refresh-data-btn"
              onClick={onRefresh}
              disabled={isRefreshing}
              title="Обновить данные"
              className="px-3 py-1.5 rounded-md bg-[#18181b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] transition flex items-center gap-1.5 text-xs font-medium cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-blue-400' : ''}`} />
              <span className="hidden sm:inline">Обновить</span>
            </button>

            <button
              id="open-settings-btn"
              onClick={onOpenSettings}
              title="Настройки подключения"
              className="p-2 rounded-md bg-[#18181b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] transition cursor-pointer"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <nav className="flex space-x-1 overflow-x-auto py-2 border-t border-[#27272a] scrollbar-none">
          {tabs.map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                id={`tab-nav-${tab.id}`}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 px-3 py-1.5 rounded-md text-xs sm:text-sm font-medium transition cursor-pointer whitespace-nowrap ${
                  isActive
                    ? 'bg-[#27272a] text-[#fafafa] font-semibold'
                    : 'text-zinc-400 hover:text-[#fafafa] hover:bg-[#18181b]'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-blue-400' : 'text-zinc-400'}`} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </nav>
      </div>
    </header>
  );
};
