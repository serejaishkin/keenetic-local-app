import React, { useState } from 'react';
import {
  Wifi,
  Lock,
  Eye,
  EyeOff,
  Copy,
  Check,
  QrCode,
  Sliders,
  Radio,
  Zap,
  Save,
  CheckCircle2,
  Share2,
  X
} from 'lucide-react';
import { WifiInterface } from '../types';

interface WifiTabProps {
  wifiList: WifiInterface[];
  onToggleWifi: (id: string, enabled: boolean) => void;
  onUpdateWifi: (id: string, updates: Partial<WifiInterface>) => void;
}

export const WifiTab: React.FC<WifiTabProps> = ({
  wifiList,
  onToggleWifi,
  onUpdateWifi,
}) => {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editSsid, setEditSsid] = useState('');
  const [editPassword, setEditPassword] = useState('KeeneticSecure2026!');
  const [showPassword, setShowPassword] = useState<{ [id: string]: boolean }>({});
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [qrModalWifi, setQrModalWifi] = useState<WifiInterface | null>(null);
  const [saveSuccessId, setSaveSuccessId] = useState<string | null>(null);

  const togglePasswordVisibility = (id: string) => {
    setShowPassword(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const startEdit = (wifi: WifiInterface) => {
    setEditingId(wifi.id);
    setEditSsid(wifi.ssid);
  };

  const saveEdit = (wifi: WifiInterface) => {
    onUpdateWifi(wifi.id, {
      ssid: editSsid,
      password: editPassword,
    });
    setEditingId(null);
    setSaveSuccessId(wifi.id);
    setTimeout(() => setSaveSuccessId(null), 2500);
  };

  // Generate QR Code SVG string using standard WIFI format
  const generateWifiQrUrl = (ssid: string, pass: string, auth: string = 'WPA') => {
    const qrData = encodeURIComponent(`WIFI:S:${ssid};T:${auth};P:${pass};;`);
    // Using clean public QR generator service with fallback SVG
    return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${qrData}&color=0f172a&bgcolor=ffffff&qzone=1`;
  };

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-xl bg-[#18181b] border border-[#27272a]">
        <div>
          <h2 className="text-base font-semibold text-[#fafafa] flex items-center gap-2">
            <Wifi className="w-5 h-5 text-blue-400" />
            Управление беспроводными сетями Wi-Fi
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Настройка диапазонов 2.4 GHz и 5 GHz (Wi-Fi 6 AX), гостевой зоны, каналов и безопасности
          </p>
        </div>

        <div className="flex items-center space-x-2 text-xs">
          <span className="px-3 py-1.5 rounded-md bg-[#09090b] text-zinc-300 font-medium border border-[#27272a]">
            Mesh-система: <strong className="text-emerald-400 font-semibold">Активна (Master)</strong>
          </span>
          <span className="px-3 py-1.5 rounded-md bg-[#09090b] text-zinc-300 font-medium border border-[#27272a]">
            Бесшовный роуминг: <strong className="text-blue-400 font-semibold">802.11r/k/v</strong>
          </span>
        </div>
      </div>

      {/* Wi-Fi Networks Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {wifiList.map(wifi => {
          const isEditing = editingId === wifi.id;
          const isPassVisible = showPassword[wifi.id];
          const defaultPass = wifi.password || 'KeeneticSecure2026!';

          return (
            <div
              key={wifi.id}
              className={`rounded-xl border transition overflow-hidden ${
                wifi.enabled
                  ? 'bg-[#18181b] border-[#27272a] shadow-sm'
                  : 'bg-[#09090b] border-[#27272a]/60 opacity-60'
              }`}
            >
              {/* Card Top Header */}
              <div className="p-5 border-b border-[#27272a] flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div
                    className={`p-2.5 rounded-lg ${
                      wifi.enabled
                        ? wifi.band === '5GHz'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                          : 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                        : 'bg-[#27272a] text-zinc-600 border border-[#27272a]'
                    }`}
                  >
                    <Radio className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center space-x-2">
                      <h3 className="font-semibold text-[#fafafa] text-sm">{wifi.name}</h3>
                      <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-[#27272a] text-zinc-300 border border-[#27272a]">
                        {wifi.band}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-400 font-mono mt-0.5">{wifi.standard}</p>
                  </div>
                </div>

                {/* Enable/Disable Toggle */}
                <div className="flex items-center space-x-2">
                  <button
                    id={`toggle-wifi-${wifi.id}`}
                    onClick={() => onToggleWifi(wifi.id, !wifi.enabled)}
                    className={`w-11 h-6 rounded-full transition p-0.5 cursor-pointer ${
                      wifi.enabled ? 'bg-blue-600' : 'bg-zinc-700'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 rounded-full bg-white transition transform ${
                        wifi.enabled ? 'translate-x-5' : 'translate-x-0'
                      }`}
                    />
                  </button>
                </div>
              </div>

              {/* Card Content & Settings */}
              <div className="p-5 space-y-4 text-xs">
                {/* SSID input */}
                <div>
                  <label className="block text-zinc-400 font-medium mb-1">Имя сети (SSID)</label>
                  {isEditing ? (
                    <input
                      type="text"
                      value={editSsid}
                      onChange={e => setEditSsid(e.target.value)}
                      className="w-full px-3 py-2 rounded-lg bg-[#09090b] border border-blue-500 text-white font-mono text-xs focus:outline-none"
                    />
                  ) : (
                    <div className="flex items-center justify-between p-2.5 rounded-lg bg-[#09090b] border border-[#27272a]">
                      <span className="font-mono font-bold text-[#fafafa] text-sm">{wifi.ssid}</span>
                      <button
                        onClick={() => startEdit(wifi)}
                        className="text-blue-400 hover:text-blue-300 font-medium cursor-pointer"
                      >
                        Изменить
                      </button>
                    </div>
                  )}
                </div>

                {/* Password field */}
                <div>
                  <label className="block text-zinc-400 font-medium mb-1">Пароль Wi-Fi</label>
                  <div className="flex items-center justify-between p-2 rounded-lg bg-[#09090b] border border-[#27272a]">
                    <div className="flex items-center space-x-2">
                      <Lock className="w-3.5 h-3.5 text-zinc-500" />
                      <span className="font-mono text-zinc-200">
                        {isPassVisible ? defaultPass : '••••••••••••••••'}
                      </span>
                    </div>

                    <div className="flex items-center space-x-1">
                      <button
                        onClick={() => togglePasswordVisibility(wifi.id)}
                        className="p-1.5 text-zinc-400 hover:text-zinc-200 rounded cursor-pointer"
                        title={isPassVisible ? 'Скрыть пароль' : 'Показать пароль'}
                      >
                        {isPassVisible ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                      </button>
                      <button
                        onClick={() => copyToClipboard(defaultPass, wifi.id)}
                        className="p-1.5 text-zinc-400 hover:text-blue-400 rounded cursor-pointer"
                        title="Скопировать пароль"
                      >
                        {copiedId === wifi.id ? (
                          <Check className="w-3.5 h-3.5 text-emerald-400" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                      </button>
                      <button
                        onClick={() => setQrModalWifi(wifi)}
                        className="p-1.5 text-zinc-400 hover:text-blue-400 rounded cursor-pointer"
                        title="Показать QR-код для подключения"
                      >
                        <QrCode className="w-3.5 h-3.5 text-blue-400" />
                      </button>
                    </div>
                  </div>
                </div>

                {/* Channel & Width Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  <div className="p-2.5 rounded-lg bg-[#09090b] border border-[#27272a]">
                    <span className="text-zinc-400 block text-[11px]">Канал</span>
                    <span className="font-mono font-bold text-[#fafafa] text-sm">
                      {wifi.channel === 'auto' ? `Авто (${wifi.actualChannel})` : wifi.channel}
                    </span>
                  </div>

                  <div className="p-2.5 rounded-lg bg-[#09090b] border border-[#27272a]">
                    <span className="text-zinc-400 block text-[11px]">Ширина канала</span>
                    <span className="font-mono font-bold text-[#fafafa] text-sm">{wifi.channelWidth} MHz</span>
                  </div>

                  <div className="p-2.5 rounded-lg bg-[#09090b] border border-[#27272a]">
                    <span className="text-zinc-400 block text-[11px]">Мощность Tx</span>
                    <span className="font-mono font-bold text-[#fafafa] text-sm">{wifi.txPower}%</span>
                  </div>
                </div>

                {/* Security Mode */}
                <div className="flex items-center justify-between p-2.5 rounded-lg bg-[#09090b] border border-[#27272a]">
                  <span className="text-zinc-400">Шифрование:</span>
                  <span className="font-semibold text-emerald-400">{wifi.security}</span>
                </div>

                {/* Advanced switches summary */}
                <div className="pt-2 border-t border-[#27272a] flex flex-wrap items-center gap-2">
                  <span className="px-2 py-1 rounded bg-[#09090b] text-zinc-300 text-[11px] border border-[#27272a]">
                    Beamforming: <strong className="text-blue-400">Вкл</strong>
                  </span>
                  <span className="px-2 py-1 rounded bg-[#09090b] text-zinc-300 text-[11px] border border-[#27272a]">
                    Band Steering: <strong className="text-blue-400">Вкл</strong>
                  </span>
                  <span className="px-2 py-1 rounded bg-[#09090b] text-zinc-300 text-[11px] border border-[#27272a]">
                    Клиентов: <strong className="text-white font-mono">{wifi.clientsCount}</strong>
                  </span>
                </div>

                {/* Edit Action Buttons */}
                {isEditing && (
                  <div className="flex justify-end space-x-2 pt-2">
                    <button
                      onClick={() => setEditingId(null)}
                      className="px-3 py-1.5 rounded-lg bg-[#27272a] text-zinc-300 hover:bg-[#3f3f46] font-medium cursor-pointer"
                    >
                      Отмена
                    </button>
                    <button
                      onClick={() => saveEdit(wifi)}
                      className="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-semibold flex items-center space-x-1 cursor-pointer"
                    >
                      <Save className="w-3.5 h-3.5" />
                      <span>Сохранить</span>
                    </button>
                  </div>
                )}

                {saveSuccessId === wifi.id && (
                  <div className="p-2 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center gap-1.5 text-[11px]">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Параметры сети сохранены в KeeneticOS!</span>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* QR Code Modal for Quick Phone Connection */}
      {qrModalWifi && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="bg-[#18181b] border border-[#27272a] rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4 relative">
            <button
              onClick={() => setQrModalWifi(null)}
              className="absolute top-4 right-4 text-zinc-400 hover:text-white p-1 rounded-lg cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center space-y-1">
              <div className="w-12 h-12 rounded-xl bg-blue-500/10 text-blue-400 flex items-center justify-center mx-auto mb-2">
                <QrCode className="w-6 h-6" />
              </div>
              <h3 className="text-base font-bold text-[#fafafa]">Подключение по QR-коду</h3>
              <p className="text-xs text-zinc-400">
                Наведите камеру смартфона для мгновенного подключения к Wi-Fi
              </p>
            </div>

            {/* QR Image Frame */}
            <div className="bg-white p-4 rounded-xl flex items-center justify-center shadow-inner">
              <img
                src={generateWifiQrUrl(qrModalWifi.ssid, qrModalWifi.password || 'KeeneticSecure2026!')}
                alt="Wi-Fi QR Code"
                className="w-48 h-48 rounded-lg"
                referrerPolicy="no-referrer"
              />
            </div>

            <div className="p-3 rounded-lg bg-[#09090b] border border-[#27272a] text-xs space-y-1 font-mono">
              <div className="flex justify-between text-zinc-400">
                <span>SSID:</span>
                <span className="text-white font-bold">{qrModalWifi.ssid}</span>
              </div>
              <div className="flex justify-between text-zinc-400">
                <span>Пароль:</span>
                <span className="text-blue-400 font-bold">{qrModalWifi.password || 'KeeneticSecure2026!'}</span>
              </div>
            </div>

            <button
              onClick={() => setQrModalWifi(null)}
              className="w-full py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition cursor-pointer"
            >
              Готово
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
