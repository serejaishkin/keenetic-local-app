import React, { useState } from 'react';
import {
  Settings,
  X,
  Server,
  Lock,
  Globe,
  Radio,
  Eye,
  EyeOff,
  Save,
  Download,
  Upload,
  RotateCcw,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { RouterConfig } from '../types';
import { DEFAULT_CONFIG } from '../services/routerService';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  config: RouterConfig;
  onSaveConfig: (config: RouterConfig) => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  config,
  onSaveConfig,
}) => {
  const [formConfig, setFormConfig] = useState<RouterConfig>(config);
  const [showPassword, setShowPassword] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  if (!isOpen) return null;

  const handleSave = () => {
    onSaveConfig(formConfig);
    setSaveSuccess(true);
    setTimeout(() => {
      setSaveSuccess(false);
      onClose();
    }, 800);
  };

  const handleReset = () => {
    setFormConfig(DEFAULT_CONFIG);
  };

  const handleExportConfig = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(formConfig, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', 'keenetic_local_config.json');
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 relative">
        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 rounded-lg bg-cyan-500/10 text-cyan-400">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Параметры подключения к Keenetic</h3>
              <p className="text-xs text-slate-400">Настройки локального RCI REST API и учетных данных</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Mode Switcher Banner */}
        <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
          <label className="text-xs font-bold text-slate-300 block">Режим работы приложения:</label>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setFormConfig(prev => ({ ...prev, isDemo: true }))}
              className={`p-2.5 rounded-lg text-xs font-semibold border text-left transition cursor-pointer ${
                formConfig.isDemo
                  ? 'bg-amber-500/20 border-amber-500/50 text-amber-300'
                  : 'bg-slate-900 border-slate-800 text-slate-400'
              }`}
            >
              <span className="block font-bold">Интерактивный симулятор</span>
              <span className="text-[10px] text-slate-400 font-normal">Работает автономно без физического роутера</span>
            </button>

            <button
              type="button"
              onClick={() => setFormConfig(prev => ({ ...prev, isDemo: false }))}
              className={`p-2.5 rounded-lg text-xs font-semibold border text-left transition cursor-pointer ${
                !formConfig.isDemo
                  ? 'bg-cyan-500/20 border-cyan-500/50 text-cyan-300'
                  : 'bg-slate-900 border-slate-800 text-slate-400'
              }`}
            >
              <span className="block font-bold">Реальный роутер (RCI)</span>
              <span className="text-[10px] text-slate-400 font-normal">Прямые запросы через Express прокси</span>
            </button>
          </div>
        </div>

        {/* Form Inputs */}
        <div className="space-y-3.5 text-xs">
          {/* IP / Host and Protocol */}
          <div className="grid grid-cols-3 gap-2.5">
            <div className="col-span-2">
              <label className="block text-slate-400 font-medium mb-1">IP адрес или домен роутера</label>
              <input
                type="text"
                value={formConfig.host}
                onChange={e => setFormConfig(prev => ({ ...prev, host: e.target.value }))}
                placeholder="192.168.1.1 или my.keenetic.net"
                className="w-full px-3 py-2 rounded-lg bg-slate-800 border border-slate-700 font-mono text-white focus:outline-none focus:border-cyan-500"
              />
            </div>

            <div>
              <label className="block text-slate-400 font-medium mb-1">Протокол</label>
              <select
                value={formConfig.protocol}
                onChange={e => setFormConfig(prev => ({ ...prev, protocol: e.target.value as any }))}
                className="w-full px-3 py-2 rounded-lg bg-slate-800 border border-slate-700 text-white focus:outline-none cursor-pointer"
              >
                <option value="http">HTTP (80)</option>
                <option value="https">HTTPS (443)</option>
              </select>
            </div>
          </div>

          {/* Username & Password */}
          <div className="grid grid-cols-2 gap-2.5">
            <div>
              <label className="block text-slate-400 font-medium mb-1">Имя пользователя (Логин)</label>
              <input
                type="text"
                value={formConfig.username}
                onChange={e => setFormConfig(prev => ({ ...prev, username: e.target.value }))}
                placeholder="admin"
                className="w-full px-3 py-2 rounded-lg bg-slate-800 border border-slate-700 font-mono text-white focus:outline-none focus:border-cyan-500"
              />
            </div>

            <div>
              <label className="block text-slate-400 font-medium mb-1">Пароль от KeeneticOS</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={formConfig.password}
                  onChange={e => setFormConfig(prev => ({ ...prev, password: e.target.value }))}
                  placeholder="Пароль администратора"
                  className="w-full px-3 py-2 pr-9 rounded-lg bg-slate-800 border border-slate-700 font-mono text-white focus:outline-none focus:border-cyan-500"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white cursor-pointer"
                >
                  {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>
          </div>

          {/* Refresh Polling Interval */}
          <div className="flex items-center justify-between p-3 rounded-lg bg-slate-800/80 border border-slate-700/60">
            <div>
              <span className="font-semibold text-white block">Интервал автообновления данных</span>
              <span className="text-[11px] text-slate-400">Частота опроса телеметрии трафика и клиентов</span>
            </div>
            <select
              value={formConfig.refreshInterval}
              onChange={e => setFormConfig(prev => ({ ...prev, refreshInterval: Number(e.target.value) }))}
              className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 text-xs text-white focus:outline-none cursor-pointer font-mono"
            >
              <option value={1}>1 сек</option>
              <option value={2}>2 сек (Рекомендуется)</option>
              <option value={5}>5 сек</option>
              <option value={10}>10 сек</option>
            </select>
          </div>
        </div>

        {/* Action Controls & Export/Reset */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-3 border-t border-slate-800 text-xs">
          <div className="flex items-center space-x-2 w-full sm:w-auto">
            <button
              onClick={handleExportConfig}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center space-x-1.5 transition cursor-pointer"
              title="Экспортировать JSON"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Экспорт</span>
            </button>

            <button
              onClick={handleReset}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 flex items-center space-x-1.5 transition cursor-pointer"
              title="Сбросить по умолчанию"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Сброс</span>
            </button>
          </div>

          <div className="flex items-center space-x-2 w-full sm:w-auto justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 font-semibold cursor-pointer"
            >
              Отмена
            </button>
            <button
              id="save-settings-btn"
              onClick={handleSave}
              className="px-5 py-2 rounded-lg bg-cyan-600 hover:bg-cyan-500 text-white font-bold transition flex items-center space-x-1.5 cursor-pointer shadow"
            >
              <Save className="w-3.5 h-3.5" />
              <span>Сохранить настройки</span>
            </button>
          </div>
        </div>

        {saveSuccess && (
          <div className="p-2 rounded-lg bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 flex items-center justify-center gap-1.5 text-xs font-semibold animate-pulse">
            <CheckCircle2 className="w-4 h-4" />
            <span>Настройки успешно сохранены!</span>
          </div>
        )}
      </div>
    </div>
  );
};
