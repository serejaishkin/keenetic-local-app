import React, { useState } from 'react';
import {
  RefreshCw,
  Settings,
  Power,
  Menu,
  CheckCircle2,
  X,
  AlertTriangle,
  Globe,
  Radio
} from 'lucide-react';
import { RouterConfig, SystemStatus, WanStatus } from '../types';

interface HeaderProps {
  config: RouterConfig;
  system: SystemStatus | null;
  wan: WanStatus | null;
  onOpenSettings: () => void;
  onRefresh: () => void;
  isRefreshing: boolean;
  onReboot: () => Promise<void>;
  onToggleMobileMenu: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  config,
  system,
  wan,
  onOpenSettings,
  onRefresh,
  isRefreshing,
  onReboot,
  onToggleMobileMenu,
}) => {
  const [showRebootModal, setShowRebootModal] = useState<boolean>(false);
  const [isRebooting, setIsRebooting] = useState<boolean>(false);

  const handleConfirmReboot = async () => {
    setIsRebooting(true);
    await onReboot();
    setTimeout(() => {
      setIsRebooting(false);
      setShowRebootModal(false);
    }, 2000);
  };

  return (
    <header className="sticky top-0 z-30 bg-[#18181b] border-b border-[#27272a] h-16">
      <div className="h-full px-4 sm:px-6 flex items-center justify-between">
        {/* Left: Mobile Menu Button & Router Info */}
        <div className="flex items-center space-x-3">
          <button
            onClick={onToggleMobileMenu}
            className="p-2 rounded-lg text-zinc-400 hover:text-white hover:bg-[#27272a] lg:hidden cursor-pointer"
            title="Открыть меню"
          >
            <Menu className="w-5 h-5" />
          </button>

          <div className="flex items-center space-x-3">
            <div className="flex flex-col">
              <div className="flex items-center space-x-2">
                <span className="text-sm font-bold text-white tracking-tight">
                  {system?.model || 'Keenetic Ultra'}
                </span>
                <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold bg-blue-500/15 text-blue-400 border border-blue-500/30">
                  KeeneticOS {system?.version || '4.2.3'}
                </span>
              </div>
              <div className="flex items-center space-x-2 text-[11px] text-zinc-400">
                <span className="flex items-center space-x-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-emerald-400 font-medium font-mono">192.168.1.1</span>
                </span>
                <span>•</span>
                <span className="font-mono text-zinc-400">{system?.uptimeFormatted || '9д 18ч'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Center: WAN status snippet on desktop */}
        <div className="hidden md:flex items-center space-x-3 text-xs">
          <div className="px-3 py-1.5 rounded-lg bg-[#09090b] border border-[#27272a] flex items-center space-x-2">
            <Globe className="w-3.5 h-3.5 text-blue-400" />
            <span className="text-zinc-400 font-medium">WAN IP:</span>
            <span className="font-mono font-bold text-white">{wan?.ip || '94.25.180.44'}</span>
          </div>

          <div className="px-3 py-1.5 rounded-lg bg-[#09090b] border border-[#27272a] flex items-center space-x-2">
            <span className="text-zinc-400 font-medium">DNS:</span>
            <span className="font-mono text-emerald-400 font-semibold">AdGuard DoH</span>
          </div>
        </div>

        {/* Right Actions */}
        <div className="flex items-center space-x-2">
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="px-3 py-1.5 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] transition flex items-center space-x-1.5 text-xs font-medium cursor-pointer"
            title="Обновить данные"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-blue-400' : ''}`} />
            <span className="hidden sm:inline">Обновить</span>
          </button>

          <button
            onClick={() => setShowRebootModal(true)}
            className="p-2 rounded-lg bg-[#09090b] hover:bg-rose-950/30 text-zinc-400 hover:text-rose-400 border border-[#27272a] hover:border-rose-500/30 transition cursor-pointer"
            title="Перезагрузить маршрутизатор"
          >
            <Power className="w-4 h-4" />
          </button>

          <button
            onClick={onOpenSettings}
            className="p-2 rounded-lg bg-[#09090b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] transition cursor-pointer"
            title="Настройки RCI API подключения"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Reboot Confirm Modal */}
      {showRebootModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-[#18181b] border border-[#27272a] rounded-xl max-w-sm w-full p-5 shadow-2xl space-y-4">
            <div className="flex items-center space-x-3 text-rose-400">
              <div className="w-10 h-10 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center shrink-0">
                <Power className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-white">Перезагрузка Keenetic</h3>
                <p className="text-xs text-zinc-400">Маршрутизатор перезагрузится через RCI</p>
              </div>
            </div>

            <p className="text-xs text-zinc-300 leading-relaxed">
              Вы уверены, что хотите перезагрузить маршрутизатор? Интернет-соединение и Wi-Fi будут временно недоступны в течение ~45 секунд.
            </p>

            <div className="flex items-center justify-end space-x-2 pt-2 border-t border-[#27272a]">
              <button
                type="button"
                onClick={() => setShowRebootModal(false)}
                disabled={isRebooting}
                className="px-4 py-2 rounded-lg bg-[#09090b] border border-[#27272a] text-zinc-300 hover:text-white text-xs cursor-pointer"
              >
                Отмена
              </button>
              <button
                type="button"
                onClick={handleConfirmReboot}
                disabled={isRebooting}
                className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition cursor-pointer flex items-center space-x-1.5"
              >
                {isRebooting && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                <span>{isRebooting ? 'Перезагрузка...' : 'Перезагрузить'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};
