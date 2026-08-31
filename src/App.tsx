import React, { useState, useEffect, useCallback } from 'react';
import { Header } from './components/Header';
import { DashboardTab } from './components/DashboardTab';
import { WifiTab } from './components/WifiTab';
import { ClientsTab } from './components/ClientsTab';
import { PortsTab } from './components/PortsTab';
import { DiagnosticsTab } from './components/DiagnosticsTab';
import { AiCopilotTab } from './components/AiCopilotTab';
import { SettingsModal } from './components/SettingsModal';
import {
  RouterConfig,
  SystemStatus,
  WanStatus,
  WifiInterface,
  ClientDevice,
  PortStatus,
  TrafficPoint,
  AiDiagnosticResult
} from './types';
import { routerService } from './services/routerService';

export default function App() {
  const [config, setConfig] = useState<RouterConfig>(routerService.getConfig());
  const [activeTab, setActiveTab] = useState<string>('dashboard');
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  // Router State
  const [system, setSystem] = useState<SystemStatus | null>(null);
  const [wan, setWan] = useState<WanStatus | null>(null);
  const [wifiList, setWifiList] = useState<WifiInterface[]>([]);
  const [clients, setClients] = useState<ClientDevice[]>([]);
  const [ports, setPorts] = useState<PortStatus[]>([]);
  const [trafficHistory, setTrafficHistory] = useState<TrafficPoint[]>([]);

  // Initial Data Fetch
  const loadAllData = useCallback(async () => {
    try {
      const [sysData, wanData, wifiData, clientsData, portsData] = await Promise.all([
        routerService.getSystemStatus(),
        routerService.getWanStatus(),
        routerService.getWifiInterfaces(),
        routerService.getClients(),
        routerService.getPorts(),
      ]);

      setSystem(sysData);
      setWan(wanData);
      setWifiList(wifiData);
      setClients(clientsData);
      setPorts(portsData);

      // Append traffic point to history
      const nowStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      setTrafficHistory(prev => {
        const next = [
          ...prev,
          {
            time: nowStr,
            downloadMbps: wanData.rxspeedMbps,
            uploadMbps: wanData.txspeedMbps,
            cpuPercent: sysData.cpuload,
            ramPercent: sysData.memPercent,
          },
        ];
        return next.length > 20 ? next.slice(next.length - 20) : next;
      });
    } catch (err) {
      console.error('Error fetching router telemetry:', err);
    }
  }, []);

  // Refresh handler
  const handleManualRefresh = async () => {
    setIsRefreshing(true);
    await loadAllData();
    setTimeout(() => setIsRefreshing(false), 500);
  };

  // Polling Effect
  useEffect(() => {
    loadAllData();

    if (!config.autoRefresh) return;
    const intervalMs = (config.refreshInterval || 2) * 1000;
    const interval = setInterval(() => {
      loadAllData();
    }, intervalMs);

    return () => clearInterval(interval);
  }, [config.autoRefresh, config.refreshInterval, config.isDemo, config.host, loadAllData]);

  // Actions
  const handleToggleWifi = async (id: string, enabled: boolean) => {
    await routerService.toggleWifi(id, enabled);
    setWifiList(prev => prev.map(w => (w.id === id ? { ...w, enabled } : w)));
  };

  const handleUpdateWifi = async (id: string, updates: Partial<WifiInterface>) => {
    await routerService.updateWifiSettings(id, updates);
    setWifiList(prev => prev.map(w => (w.id === id ? { ...w, ...updates } : w)));
  };

  const handleToggleBlockClient = async (mac: string, blocked: boolean) => {
    await routerService.setClientBlocked(mac, blocked);
    setClients(prev => prev.map(c => (c.mac === mac ? { ...c, blocked } : c)));
  };

  const handleSetSpeedLimit = async (mac: string, limitKbps: number) => {
    await routerService.setClientSpeedLimit(mac, limitKbps);
    setClients(prev => prev.map(c => (c.mac === mac ? { ...c, speedLimitKbps: limitKbps } : c)));
  };

  const handleUpdateClientInfo = async (mac: string, name: string, dhcpStatic: boolean, ip?: string) => {
    await routerService.updateClientInfo(mac, name, dhcpStatic, ip);
    setClients(prev =>
      prev.map(c => (c.mac === mac ? { ...c, customName: name, dhcpStatic, ip: ip || c.ip } : c))
    );
  };

  const handleRebootRouter = async () => {
    await routerService.rebootRouter();
    await loadAllData();
  };

  const handleReconnectWan = async () => {
    await routerService.reconnectWan();
    await loadAllData();
  };

  const handleExecuteRci = async (path: string, method: 'GET' | 'POST', body?: any) => {
    return await routerService.executeRawRci(path, method, body);
  };

  const handleRunAiDiagnostics = async (): Promise<AiDiagnosticResult> => {
    const snapshot = {
      system,
      wan,
      wifiList,
      clientsCount: clients.length,
      activeClients: clients.filter(c => c.online && !c.blocked),
      ports,
    };
    return await routerService.runAiDiagnostics(snapshot);
  };

  const handleAskAiCopilot = async (question: string): Promise<string> => {
    const routerState = {
      system,
      wan,
      wifi24: wifiList.find(w => w.band === '2.4GHz'),
      wifi5: wifiList.find(w => w.band === '5GHz'),
      clients,
    };
    return await routerService.askAiCopilot(question, routerState, []);
  };

  const handleSaveConfig = (newConfig: RouterConfig) => {
    routerService.setConfig(newConfig);
    setConfig(newConfig);
    loadAllData();
  };

  return (
    <div className="min-h-screen bg-[#09090b] text-[#fafafa] flex flex-col font-sans selection:bg-blue-500 selection:text-white">
      {/* Top Fixed Header */}
      <Header
        config={config}
        system={system}
        wan={wan}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenSettings={() => setIsSettingsOpen(true)}
        onRefresh={handleManualRefresh}
        isRefreshing={isRefreshing}
      />

      {/* Main Tab Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'dashboard' && (
          <DashboardTab
            system={system}
            wan={wan}
            wifiList={wifiList}
            clients={clients}
            ports={ports}
            trafficHistory={trafficHistory}
            onToggleWifi={handleToggleWifi}
            onReboot={handleRebootRouter}
            onReconnectWan={handleReconnectWan}
            onNavigateToTab={setActiveTab}
          />
        )}

        {activeTab === 'wifi' && (
          <WifiTab
            wifiList={wifiList}
            onToggleWifi={handleToggleWifi}
            onUpdateWifi={handleUpdateWifi}
          />
        )}

        {activeTab === 'clients' && (
          <ClientsTab
            clients={clients}
            onToggleBlock={handleToggleBlockClient}
            onSetSpeedLimit={handleSetSpeedLimit}
            onUpdateClient={handleUpdateClientInfo}
          />
        )}

        {activeTab === 'ports' && (
          <PortsTab
            ports={ports}
            wan={wan}
          />
        )}

        {activeTab === 'diagnostics' && (
          <DiagnosticsTab
            onExecuteRci={handleExecuteRci}
          />
        )}

        {activeTab === 'ai' && (
          <AiCopilotTab
            system={system}
            wan={wan}
            wifiList={wifiList}
            clients={clients}
            onRunDiagnostics={handleRunAiDiagnostics}
            onAskCopilot={handleAskAiCopilot}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-[#27272a] py-4 bg-[#09090b] text-xs text-zinc-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center space-x-2">
            <span className="font-semibold text-zinc-300">Keenetic Local</span>
            <span>•</span>
            <span>Управление роутерами через REST RCI API</span>
          </div>
          <div className="flex items-center space-x-3 text-[11px] font-mono text-zinc-400">
            <span>Модель: {system?.model || 'Keenetic Ultra'}</span>
            <span>•</span>
            <span>KeeneticOS: {system?.version || '4.2.3'}</span>
          </div>
        </div>
      </footer>

      {/* Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        config={config}
        onSaveConfig={handleSaveConfig}
      />
    </div>
  );
}
