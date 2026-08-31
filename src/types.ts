export interface RouterConfig {
  host: string;
  port: string;
  protocol: 'http' | 'https';
  username: string;
  password: string;
  isDemo: boolean;
  autoRefresh: boolean;
  refreshInterval: number; // in seconds
}

export interface SystemStatus {
  model: string;
  device: string;
  version: string;
  release: string;
  uptime: number; // in seconds
  uptimeFormatted: string;
  cpuload: number; // percentage 0-100
  memfree: number; // in KB
  memtotal: number; // in KB
  memused: number; // in KB
  memPercent: number;
  hostname: string;
  ndmVersion: string;
  hw_version: string;
  firmwareDate: string;
  cpuCores: number;
  cpuFreq: string;
}

export interface WanStatus {
  interface: string;
  connected: boolean;
  ip: string;
  mask: string;
  gateway: string;
  dns: string[];
  uptime: number;
  rxbytes: number;
  txbytes: number;
  rxspeed: number; // in bytes/sec
  txspeed: number; // in bytes/sec
  rxspeedMbps: number;
  txspeedMbps: number;
  mac: string;
  isp: string;
  linkSpeed: string;
}

export interface WifiInterface {
  id: string;
  name: string;
  band: '2.4GHz' | '5GHz';
  standard: string; // '802.11ax (Wi-Fi 6)' | '802.11ac'
  enabled: boolean;
  ssid: string;
  password?: string;
  security: 'WPA2-PSK' | 'WPA3-SAE' | 'WPA2/WPA3 Mixed' | 'Open';
  channel: number | 'auto';
  actualChannel: number;
  channelWidth: '20' | '40' | '80' | '160';
  txPower: number; // 0-100%
  hidden: boolean;
  clientsCount: number;
  bandSteering: boolean;
  beamforming: boolean;
  fastRoaming: boolean; // 802.11r/k/v
}

export interface ClientDevice {
  mac: string;
  ip: string;
  hostname: string;
  customName?: string;
  interface: string;
  connectionType: 'wifi24' | 'wifi5' | 'eth' | 'mesh';
  online: boolean;
  blocked: boolean;
  speedLimitKbps: number; // 0 = unlimited
  rxSpeed: number; // bytes/sec
  txSpeed: number; // bytes/sec
  rxBytes: number;
  txBytes: number;
  rssi?: number; // dBm e.g. -54
  linkRate?: number; // Mbps e.g. 866 or 1200
  firstSeen: string;
  lastSeen: string;
  vendor: string;
  iconType: 'phone' | 'laptop' | 'tv' | 'smart_home' | 'console' | 'tablet' | 'printer' | 'server' | 'other';
  dhcpStatic: boolean;
  port?: number; // for ethernet
}

export interface PortStatus {
  portNumber: number;
  label: string;
  type: 'WAN' | 'LAN';
  speed: '1000M' | '100M' | '10M' | 'Down';
  duplex: 'Full' | 'Half' | 'None';
  link: boolean;
  rxRateMbps: number;
  txRateMbps: number;
  connectedDevice?: string;
}

export interface TrafficPoint {
  time: string;
  downloadMbps: number;
  uploadMbps: number;
  cpuPercent: number;
  ramPercent: number;
}

export interface PingResult {
  host: string;
  avgMs: number;
  minMs: number;
  maxMs: number;
  loss: number; // percentage
  packetsSent: number;
  packetsReceived: number;
  history: number[];
  timestamp: string;
  status: 'success' | 'running' | 'failed';
}

export interface TracerouteHop {
  hop: number;
  ip: string;
  host: string;
  rtt1: number;
  rtt2: number;
  rtt3: number;
  loss: boolean;
}

export interface TracerouteResult {
  target: string;
  hops: TracerouteHop[];
  status: 'running' | 'completed' | 'failed';
  timestamp: string;
}

export interface RciCommandLog {
  id: string;
  path: string;
  method: 'GET' | 'POST' | 'DELETE';
  body?: string;
  status: number;
  response: any;
  timestamp: string;
  durationMs: number;
}

export interface SyslogEntry {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARNING' | 'ERROR' | 'NOTICE';
  facility: 'dhcpd' | 'ndm' | 'kernel' | 'wpa_supplicant' | 'dnsmasq' | 'wireguard' | 'upnp' | 'system';
  message: string;
}

export interface PortForwardingRule {
  id: string;
  name: string;
  protocol: 'TCP' | 'UDP' | 'TCP/UDP';
  externalPort: string;
  internalIp: string;
  internalPort: string;
  enabled: boolean;
  comment?: string;
}

export interface DnsFilterConfig {
  mode: 'none' | 'adguard' | 'nextdns' | 'cloudflare' | 'yandex_safe' | 'yandex_family' | 'custom';
  customDnsPrimary: string;
  customDnsSecondary: string;
  dohEnabled: boolean;
  dohServer: string;
  blockedQueriesCount: number;
  totalQueriesCount: number;
}

export interface VpnConnection {
  id: string;
  name: string;
  type: 'WireGuard' | 'OpenVPN' | 'SSTP' | 'IPsec';
  status: 'connected' | 'disconnected' | 'connecting';
  serverAddress: string;
  clientIp: string;
  txBytes: number;
  rxBytes: number;
  rxSpeed: number;
  txSpeed: number;
  uptime: string;
  policy: 'all' | 'specific_domains' | 'selected_devices';
  devicesCount: number;
}

export interface AiDiagnosticResult {
  healthScore: number;
  statusRating: 'Optimal' | 'Good' | 'Warning' | 'Critical';
  summary: string;
  wifiRecommendations: string[];
  securityFindings: string[];
  performanceInsights: string[];
  quickActions: {
    title: string;
    description: string;
    impact: 'High' | 'Medium' | 'Low';
  }[];
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant' | 'system';
  text: string;
  timestamp: string;
  suggestedAction?: {
    label: string;
    command?: string;
  };
}
