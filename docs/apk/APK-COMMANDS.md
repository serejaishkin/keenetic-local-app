# Команды RCI из оригинального приложения (авто-извлечение из декомпилята)
Каждая строка — цепочка CommandBuilder: `show skydns userinfo` = {"show":{"skydns":{"userinfo":{}}}}; `GET /rci/...` — прямой GET. `<имя>` — значение из переменной. Это ФОРМАТ ЗАПРОСОВ (чтение и запись), не ответов. Статус 🔴 до проверки на роутере.

## knext/commands/command/base/SystemConfigSaveCommand.java
- `configuration`

## knext/commands/command/base/builder/CommandBuilder.java
- `<name)>`

## knext/commands/command/base/interfaceCmds/ClearWispSettingsCommand.java
- `<ADDRESS)>`
- `<addCommand(new>`
- `<getName)>`
- `authentication`
- `description`
- `dhcp`
- `enable`
- `encryption`
- `gateway`
- `global`
- `ip`
- `name-server`
- `ping-check`
- `profile`
- `ssid`
- `up`
- `wpa`
- `wpa-psk`
- `wpa2`
- `wps`

## knext/commands/command/base/interfaceCmds/PingCheckCommand.java
- `<profile)>`
- `max-fails`
- `profile`
- `update-interval`

## knext/commands/command/base/interfaceCmds/PingCheckOffCommand.java
- `<profileName)>`
- `ping-check`
- `profile`

## knext/commands/command/base/interfaceCmds/SaveIpCommand.java
- `<>`

## knext/commands/command/base/interfaceCmds/SaveIpoeCommand.java
- `<ADDRESS)>`
- `<getName)>`
- `<inetInterfaceId)>`
- `<interfaceName)>`
- `<str3)>`
- `authentication`
- `client`
- `clone`
- `configuration`
- `connect`
- `dhcp`
- `global`
- `interface`
- `ip`
- `mac`
- `name-servers`
- `schedule`
- `tcp`

## knext/commands/command/base/interfaceCmds/SaveL2tpCommand.java
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `mtu`
- `peer`
- `schedule`
- `tcp`
- `up`

## knext/commands/command/base/interfaceCmds/SaveModemCommand.java
- `<>`
- `<addCommand(new>`
- `<addParam(>`
- `<getName)>`
- `adjust-ttl`
- `apn`
- `authentication`
- `connect`
- `global`
- `identity`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `ipv6`
- `ipv6 <DEBUG_PROPERTY_VALUE_AUTO> prefix name-servers`
- `lte`
- `mobile`
- `modem`
- `nr5g`
- `operator`
- `schedule`
- `tty modem`
- `umts`
- `usb`

## knext/commands/command/base/interfaceCmds/SaveOpenVpnCommand.java
- `<addCommand(new>`
- `<addParam(>`
- `<getName)>`
- `adjust-mss`
- `config`
- `connect`
- `global`
- `ip`
- `openvpn`
- `schedule`
- `tcp`
- `up`

## knext/commands/command/base/interfaceCmds/SavePppoeCommand.java
- `<CATEGORY_SERVICE)>`
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `ipcp`
- `peer`
- `pppoe`
- `tcp`

## knext/commands/command/base/interfaceCmds/SavePptpCommand.java
- `adjust-mss`
- `authentication`
- `connect`
- `encryption`
- `global`
- `mtu`
- `peer`
- `schedule`
- `tcp`
- `up`

## knext/commands/command/base/interfaceCmds/SaveWispCommand.java
- `<>`
- `<ADDRESS)>`
- `<addCommand(new>`
- `<getMasterName)>`
- `<getName)>`
- `<inetInterfaceId)>`
- `adjust-ttl`
- `authentication wpa-psk`
- `bssid`
- `enable`
- `encryption`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `mac`
- `name-servers`
- `owe`
- `schedule`
- `ssid`
- `up`
- `wpa`
- `wpa2`
- `wpa3`

## knext/commands/command/base/interfaceCmds/SetInterfaceIpPrioCommand.java
- `<addCommand(new> global`

## knext/commands/command/router/applications/ChangeApplicationActiveStatusCommand.java
- `<addCommand(new>`

## knext/commands/command/router/applications/ChangeDlnaDbDirectoryCommand.java
- `dlna db-directory`

## knext/commands/command/router/applications/ChangeDlnaDirectoryCommand.java
- `dlna`

## knext/commands/command/router/applications/ChangeDlnaIncludeSegListCommand.java
- `dlna`

## knext/commands/command/router/applications/DlnaCommand.java
- `rc dlna`

## knext/commands/command/router/applications/OCServerCmd.java
- `<addParam(>`
- `config`
- `oc-server`
- `users`

## knext/commands/command/router/applications/SaveDlnaCommand.java
- `<>`
- `<addCommand(new>`
- `db-directory`
- `dlna`
- `dlna directory`

## knext/commands/command/router/applications/ShowDlnaCommand.java
- `dlna`

## knext/commands/command/router/applications/WireguardServerCreatePeersCmd.java
- `config`
- `peers`

## knext/commands/command/router/applications/WireguardServerSaveCmd.java
- `config`
- `wireguard-server`

## knext/commands/command/router/applications/WireguardServerSavePeersCmd.java
- `config`
- `wireguard-server`

## knext/commands/command/router/connections/Ipv6ToIpv4TunnelCmd.java
- `<ifaceName)>`
- `global`
- `interface`
- `ip`
- `tunnel`

## knext/commands/command/router/connections/OpenConnectCmd.java
- `<addNoTrueParam)>`
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `interface`
- `ip`
- `mtu`
- `openconnect`
- `schedule`
- `tcp`
- `upstream`

## knext/commands/command/router/connections/ProxyCmd.java
- `<addNoTrueParam)>`
- `<ifaceName)>`
- `authentication`
- `connect`
- `global`
- `identity`
- `interface`
- `ip`
- `protocol`
- `proxy`
- `schedule`
- `upstream`

## knext/commands/command/router/dns/AddDnsOverHttpsCommand.java
- `dns-proxy`
- `https`
- `upstream`

## knext/commands/command/router/dns/AddDnsOverTlsCommand.java
- `dns-proxy`
- `tls`
- `upstream`

## knext/commands/command/router/dns/RemoveDnsOverHttpsCommand.java
- `dns-proxy https upstream`

## knext/commands/command/router/dns/RemoveDnsOverTlsCommand.java
- `dns-proxy`
- `tls`
- `upstream`

## knext/commands/command/router/firmware/ChangeAutoUpdateSandboxCommand.java
- `<addCommand(new>`
- `components auto-update`

## knext/commands/command/router/firmware/SetAutoUpdateSchedule.java
- `auto-update`
- `components`
- `schedule`

## knext/commands/command/router/firmware/SetFirmwareAutoUpdateStatusCommand.java
- `components auto-update disable`

## knext/commands/command/router/host/KnownHostCommand.java
- `host`
- `known`

## knext/commands/command/router/iFace/StandbyCommand.java
- `timeout`

## knext/commands/command/router/iFace/wireguard/SaveWireguardCommand.java
- `<ifaceName)>`
- `adjust-mss`
- `endpoint`
- `global`
- `interface`
- `keepalive-interval`
- `listen-port`
- `mtu`
- `preshared-key`
- `schedule`
- `tcp`
- `up`
- `wireguard`

## knext/commands/command/router/internetSafety/FirewallRuleControlCommand.java
- `<INDEX>`
- `<addCommand(new>`
- `<str)>`
- `access-group`
- `access-list`
- `access-list rule`
- `description`
- `interface`
- `ip`
- `schedule`

## knext/commands/command/router/internetSafety/SaveInternetSafetyServiceCommand.java
- `<addParam(>`
- `<getServiceName)>`

## knext/commands/command/router/internetSafety/SetInternetSafetyProfileToConnectedDeviceCommand.java
- `<getServiceName)>`

## knext/commands/command/router/ip/AddNameServerCommand.java
- `ip`

## knext/commands/command/router/ip/ClearIpRouteCommand.java
- `ip`
- `route`

## knext/commands/command/router/ip/ClearIpStaticCommand.java
- `ip static`

## knext/commands/command/router/ip/DeleteIpStaticCommand.java
- `ip static`

## knext/commands/command/router/ip/RemoveNameServerCommand.java
- `ip`

## knext/commands/command/router/ip/SaveAccessSettingsCommand.java
- `http`
- `ip`
- `security-level`
- `ssh`
- `ssl`
- `telnet`

## knext/commands/command/router/ip/SaveIpRouteCommand.java
- `ip`
- `route`

## knext/commands/command/router/ip/SaveIpStaticCommand.java
- `ip`
- `schedule`
- `static`

## knext/commands/command/router/ip/SetConnectedDeviceIpCommand.java
- `host`
- `ip dhcp`

## knext/commands/command/router/ip/TrafficShapeHostCommand.java
- `host`
- `ip`
- `traffic-shape`

## knext/commands/command/router/ip/TrafficShapeUnknownHostCommand.java
- `ip`
- `traffic-shape`
- `unknown-host`

## knext/commands/command/router/segment/InterfaceIsegCommand.java
- `<getName)>`
- `iseg`

## knext/commands/command/router/segment/LinkAggregationsCommand.java
- `<addCommand(new>`

## knext/commands/command/router/segment/RemoveSegmentNetworkCommand.java
- `<interfaceName)>`
- `encryption authentication wpa-psk ft`

## knext/commands/command/router/segment/SegmentCreateCommand.java
- `<addParam(> security-level iseg ip <ADDRESS>`

## knext/commands/command/router/segment/SegmentCreateNotRouterCommand.java
- `<addParam(> iseg`

## knext/commands/command/router/segment/SegmentCreateSetDhcpConfigCommand.java
- `bind`
- `default-router`
- `dhcp`
- `dns-server`
- `nat`
- `pool`
- `range`

## knext/commands/command/router/segment/SegmentDeleteCommand.java
- `<addCommand(new>`

## knext/commands/command/router/segment/SegmentRenameCommand.java
- `<addParam(>`

## knext/commands/command/router/segment/SegmentSetAdditionalModeIpCommand.java
- `<ADDRESS)>`
- `<segmentInnerName)>`
- `dhcp client fallback`
- `ip`

## knext/commands/command/router/segment/SegmentSetBandSteeringCommand.java
- `<>`
- `<> preference`
- `<iFaceName)>`
- `band-steering`

## knext/commands/command/router/segment/SegmentSetDhcpConfigCommand.java
- `bind`
- `default-router`
- `dhcp`
- `dns-server`
- `lan`
- `pool`
- `range`
- `relay`
- `server`

## knext/commands/command/router/segment/SegmentSetEapRadiusCommand.java
- `<addCommand(new> radius server`

## knext/commands/command/router/segment/SegmentSetIgmpCommand.java
- `<addCommand(new>`
- `<addCommand(new> downstream`
- `<addNoFalseCommand(>`
- `force`
- `igmp-proxy`
- `interface`

## knext/commands/command/router/segment/SegmentSetNatCommand.java
- `nat`

## knext/commands/command/router/segment/SegmentSetRestrictionsCommand.java
- `<getInnerName)>`
- `traffic-shape`

## knext/commands/command/router/segment/SegmentSetRoamingCommand.java
- `<addParam(>`
- `ft`
- `interface`
- `interface <addCommand(new>`

## knext/commands/command/router/segment/SegmentSetRouterIpCommand.java
- `<addCommand(new> <ADDRESS>`

## knext/commands/command/router/segment/SegmentWifiAccessPointCommand.java
- `<getInterfaceName)>`
- `authentication wpa-psk`
- `auto-self-pin`
- `enable`
- `encryption`
- `hide-ssid`
- `owe`
- `schedule`
- `ssid`
- `up`
- `wpa`
- `wpa2`
- `wpa3`

## knext/commands/command/router/segment/SetPPPoEPassthroughCommand.java
- `pass`

## knext/commands/command/router/segment/SetUnregisteredDevicesConnectionPolicyCommand.java
- `hotspot`

## knext/commands/command/router/segment/SetupSegmentIsegParamCommand.java
- `<bridgeName)>`
- `iseg`

## knext/commands/command/router/segment/mwsWlan/MwsWlanCommand.java
- `band`
- `band-steering`
- `bind`
- `enable`
- `encryption`
- `ft`
- `iapp`
- `key`
- `mdid`
- `mlo`
- `prefer-band`
- `rrm`
- `schedule`
- `skip-band`
- `ssid`
- `wlan`
- `wpa`
- `wpa eap radius server`
- `wps`

## knext/commands/command/router/segment/mwsWlan/RemoveMwsWlanCommand.java
- `wlan`

## knext/commands/command/router/segment/mwsWlan/SaveMwsWlanMultiCommand.java
- `band`
- `bind`
- `mws`

## knext/commands/command/router/segment/mwsWlan/SyncMwsWlanRadiusCommand.java
- `wpa eap radius server`

## knext/commands/command/router/system/StartDeviceCoalaAgent.java
- `cloud control2 security-level`

## knext/commands/command/router/system/SystemResetCommand.java
- `<addCommand(new>`
- `<addCommand(new> factory-reset`

## knext/commands/command/router/usb/MountUsbPartitionsCommand.java
- `<addCommand(new>`

## knext/commands/command/router/usb/UnmountUsbPartitionsCommand.java
- `<addCommand(new>`

## knext/commands/command/router/user/DeleteUser.java
- `<addCommand(new>`
- `<addParam(>`
- `user`

## knext/commands/command/router/user/SaveUser.java
- `<addCommand(new>`
- `<getName)>`
- `home`
- `user`

## knext/commands/command/router/wifi/StartWpsCommand.java
- `interface <addCommand(new> button`
- `interface <addParam(>`

## knext/commands/command/router/wifi/StopWpsCommand.java
- `interface <addCommand(new>`

## knext/helpers/ktExtensions/commandBuilder/CommandBuilderExtensionKt.java
- `ipv6cp`

## knext/managers/CryptoManager.java
- `GET /rci/rc/crypto/l2tp-server`
- `GET /rci/rc/crypto/map/VPNL2TPServer`
- `GET /rci/rc/crypto/map/VirtualIPServer`
- `GET /rci/rc/crypto/map/VirtualIPServerIKE2`
- `GET /rci/rc/crypto/virtual-ip-server`
- `GET /rci/rc/crypto/virtual-ip-server-ikev2`

## knext/managers/DeviceServiceControlManager.java
- `GET /rci/show/`
- `GET /rci/show/ip/name-server`
- `GET /rci/show/rc/`

## knext/managers/DlnaManager.java
- `GET /rci/ rc <addCommand(new> version`
- `GET /rci/ls/entry`
- `POST /rci/dlna/rescan`
- `ls`

## knext/managers/DnsConfigurationManager.java
- `dns-proxy`
- `dns-proxy filter profile`
- `dns53`
- `filter`
- `https`
- `intercept`
- `ip`
- `ipv6`
- `profile`
- `tls`

## knext/managers/DslManager.java
- `GET /rci/interface/`
- `GET /rci/show/interface/dsl/diagnostics?name=`
- `GET /rci/show/interface/dsl/disconnect-report?name=`
- `interface <addCommand(new>`

## knext/managers/IntelliQoSManager.java
- `<CATEGORY_SERVICE)>`
- `GET /rci/show/ntce/groups`
- `GET /rci/show/ntce/groupsets`
- `GET /rci/show/rc/ntce/qos`
- `host`
- `hotspot`
- `ip`
- `ntce`
- `qos`

## knext/managers/KeenDnsManager.java
- `GET /rci/ndns/book-name`
- `GET /rci/ndns/check-name`
- `GET /rci/ndns/drop-name`
- `GET /rci/ndns/get-update`
- `POST /rci/ndns/book-name`
- `POST /rci/ndns/check-name`
- `POST /rci/ndns/drop-name`
- `POST /rci/ndns/get-booked`
- `POST /rci/ndns/get-update`
- `http`
- `ip`
- `ip http ssl acme list`
- `security-level`
- `show acme`
- `show ndns`
- `show rc ip http`

## knext/managers/KeeneticLegalDocumentsManager.java
- `GET /rci/show/`
- `POST /rci/show/dpn/document`
- `POST /rci/show/eula/document`
- `dpn accept`
- `eula accept`

## knext/managers/MultipleDeviceControlManager.java
- `GET /rci/show/rc/service`

## knext/managers/MwsManager.java
- `GET /rci/mws/acquire`
- `GET /rci/mws/member`
- `GET /rci/mws/revisit`
- `GET /rci/mws/zone`
- `GET /rci/show/mws candidate member`
- `GET /rci/show/mws/log`
- `GET /rci/show/mws/member`
- `POST /rci/`
- `zone`

## knext/managers/ScheduleManager.java
- `<>`
- `GET /rci/schedule`
- `GET /rci/show/schedule`
- `POST /rci/ <getId>`
- `POST /rci/ <scheduleId>`
- `POST /rci/show/rc/schedule`
- `host`
- `hotspot`
- `ip`

## knext/managers/TorrentManager.java
- `GET /rci/show/rc/service`
- `GET /rci/show/torrent/status`
- `GET /rci/torrent`
- `POST /rci/ls`
- `POST /rci/torrent`
- `POST /rci/torrent/rpc`
- `arguments`
- `peer-port`
- `rpc-port`

## knext/managers/UsbManager.java
- `GET /rci/show/media`
- `GET /rci/show/usb`
- `POST /rci/system/eject`

## knext/managers/UsbModemManager.java
- `GET /rci/interface/`
- `GET /rci/show/interface/bands?name=`
- `GET /rci/show/interface/operators?name=`

## knext/managers/account/ConnectedDevicesManager.java
- `GET /rci/show/associations`
- `GET /rci/show/rc/ip/hotspot/policy`
- `host`
- `ip hotspot`

## knext/managers/applications/AfpManager.java
- `<SYSTEM_PROFILE_ID)>`
- `<addCommand(new>`
- `GET /rci/show/afp`
- `afp`
- `afp <addNoTrueParam))>`
- `automount`
- `permissive`

## knext/managers/applications/ApplicationStatisticManager.java
- `GET /rci/show/crypto/map/crypto_map/`
- `GET /rci/show/vpn-server`

## knext/managers/applications/CifsManager.java
- `<SYSTEM_PROFILE_ID)>`
- `<addCommand(new>`
- `GET /rci/show/cifs`
- `automount`
- `cifs`
- `cifs <addNoTrueParam))>`
- `permissive`

## knext/managers/applications/FtpManager.java
- `<addCommand(new>`
- `GET /rci/show/ip/ftp`
- `GET /rci/show/rc/ip/ftp`
- `ftp`
- `ip`
- `permissive`
- `root`
- `security-level`

## knext/managers/applications/IKEv2Manager.java
- `<addCommand(new>`
- `<getName)>`
- `crypto`
- `map`
- `static-ip`
- `virtual-ip`
- `virtual-ip-server-ikev2`

## knext/managers/applications/IpsecL2tpManager.java
- `<addCommand(new>`
- `<getName)>`
- `crypto`
- `l2tp-server`
- `map`
- `static-ip`

## knext/managers/applications/IpsecManager.java
- `<addCommand(new>`
- `<getName)>`
- `crypto`
- `map`
- `static-ip`
- `virtual-ip`
- `virtual-ip-server`

## knext/managers/applications/OpenConnectManager.java
- `GET /rci/show/oc-server`
- `GET /rci/show/rc/oc-server`
- `oc-server config`

## knext/managers/applications/PptpManager.java
- `<addCommand(new>`
- `<getName)>`
- `GET /rci/show/rc/ip/nat`
- `GET /rci/show/rc/vpn-server`
- `ip`
- `nat`
- `pool-range`
- `static-ip`
- `vpn`
- `vpn-server`

## knext/managers/applications/SFtpManager.java
- `<addCommand(new>`
- `GET /rci/show/rc/ip/ssh`
- `enable`
- `ip`
- `permissive`
- `root`
- `security-level`
- `sftp`
- `ssh`

## knext/managers/applications/SstpManager.java
- `<addCommand(new>`
- `<getName)>`
- `GET /rci/ip/http/ssl/`
- `GET /rci/show/rc/ip/nat`
- `GET /rci/show/sstp-server`
- `GET /rci/sstp-server`
- `http`
- `ip`
- `nat`
- `pool-range`
- `ssl`
- `sstp`
- `sstp-server`
- `static-ip`

## knext/managers/applications/UdpProxyManager.java
- `<addCommand(new>`
- `GET /rci/rc/udpxy`
- `interface`
- `udpxy`

## knext/managers/applications/WebDavManager.java
- `GET /rci/show/rc/ip/http`
- `enable`
- `http`
- `ip`
- `permissive`
- `root`
- `security-level`
- `webdav`

## knext/managers/applications/WireguardServerManager.java
- `GET /rci/show/`
- `wireguard-server config`
- `wireguard-server config peers delete`

## knext/managers/connections/DslManager.java
- `<ADDRESS)>`
- `<DSL_INTERFACE_NAME)>`
- `<addCommand(new>`
- `<addCommand(new> <addCommand(new>`
- `<addCommand(new> ping-check`
- `<addNoTrueParam)>`
- `<getCode)>`
- `<getInterfaceName)>`
- `<getName)>`
- `<inetInterfaceId)>`
- `<profile)>`
- `<str2)>`
- `<str27)>`
- `<str29)>`
- `<str3)>`
- `<str30)>`
- `<str32)>`
- `<str5)>`
- `<str6)>`
- `<str7)>`
- `GET /rci/interface/ipvc?parent=Dsl0`
- `POST /rci/show/interface/dsl/capabilities?name=Dsl0`
- `client`
- `connect`
- `dhcp`
- `dsl`
- `global`
- `gvector`
- `interface`
- `ip`
- `ipcp`
- `ipv6`
- `ipvc`
- `max-fails`
- `name-servers`
- `operating-mode`
- `ping-check`
- `ping-check profile`
- `prefix`
- `profile`
- `schedule`
- `tcp`
- `up`
- `update-interval`

## knext/managers/connections/IKEv1Manager.java
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `ipsec`
- `mtu`
- `preshared-key`
- `schedule`
- `tcp`
- `tunnel`
- `up`

## knext/managers/connections/IKEv2Manager.java
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `ipsec`
- `schedule`
- `tcp`
- `tunnel`
- `up`

## knext/managers/connections/L2TPIPSECManager.java
- `<addNoTrueParam)>`
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `ipsec`
- `mtu`
- `preshared-key`
- `schedule`
- `tcp`
- `up`

## knext/managers/connections/L2TPManager.java
- `<addNoTrueParam)>`
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `mtu`
- `schedule`
- `tcp`
- `up`

## knext/managers/connections/SSTPManager.java
- `<addNoTrueParam)>`
- `<ifaceName)>`
- `adjust-mss`
- `authentication`
- `connect`
- `global`
- `interface`
- `interface <addCommand(new>`
- `ip`
- `mtu`
- `schedule`
- `tcp`
- `up`

## knext/managers/connections/SmsManager.java
- `list`
- `sms`

## knext/managers/connections/UsbDslManager.java
- `<ADDRESS)>`
- `<USB_DSL_INTERFACE_NAME)>`
- `<addCommand(new>`
- `<addCommand(new> ip global`
- `<addCommand(new> ping-check`
- `<addNoTrueCommand(interfaceName))>`
- `<getCode)>`
- `<getInterfaceName)>`
- `<inetInterfaceId)>`
- `<interfaceName)>`
- `<profile)>`
- `<str)>`
- `<str10)>`
- `<str11)>`
- `<str2)>`
- `<str21)>`
- `<str5)>`
- `<str6)>`
- `<str7)>`
- `<str8)>`
- `GET /rci/interface/ipoe?parent=UsbDsl0`
- `GET /rci/interface/ipvc?parent=UsbDsl0`
- `POST /rci/show/interface/dsl/capabilities?name=UsbDsl0`
- `client`
- `connect`
- `dhcp`
- `global`
- `interface`
- `interface <addCommand(new> profile`
- `interface <addNoTrueCommand(>`
- `interface ping-check profile`
- `ip`
- `ipoe`
- `ipv6`
- `ipvc`
- `max-fails`
- `name-servers`
- `operating-mode`
- `ping-check`
- `ping-check profile <addNoTrueParam)))>`
- `prefix`
- `profile`
- `schedule`
- `tcp`
- `up`
- `update-interval`
- `vdsl`

## knext/managers/connections/WireguardManager.java
- `<str)>`
- `GET /rci/ <addCommand(new>`
- `interface`

## knext/managers/contentFilters/DnsManager.java
- `GET /rci/dns-proxy`
- `dns-proxy`
- `engine`
- `filter`

## knext/managers/contentFilters/PaidDnsManager.java
- `<addParam(>`
- `<getCode)>`
- `GET /rci/show/nextdns/profiles/profiles`
- `GET /rci/show/rc/nextdns`
- `GET /rci/show/rc/safedns`
- `GET /rci/show/rc/skydns`
- `GET /rci/show/safedns/profiles/profiles`
- `GET /rci/show/skydns/profiles/profiles`
- `nextdns authenticate`
- `nextdns authtoken`
- `show <addCommand(new>`
- `show rc <getCode))))>`

## knext/managers/contentFilters/PublicDnsManager.java
- `<str5)>`
- `GET /rci/show/rc/dns-proxy/filter/assign`
- `assign`
- `dns-proxy`
- `filter`
- `preset`
- `presets`
- `profile`
- `show`
- `show rc dns-proxy filter profile`

## knext/managers/deviceControl/DeviceControlManager.java
- `<>`
- `<getInterfaceName)>`
- `<getServiceName))>`
- `GET /rci/access-list`
- `GET /rci/interface/`
- `GET /rci/ip`
- `GET /rci/ip/dhcp/host`
- `GET /rci/ip/http/ssl/acme/list`
- `GET /rci/ip/route`
- `GET /rci/ip/static`
- `GET /rci/ip/traffic-shape/unknown-host`
- `GET /rci/service`
- `GET /rci/show/`
- `GET /rci/show/acme`
- `GET /rci/show/interface/wps/status/wps?name=`
- `GET /rci/show/ip/arp`
- `GET /rci/show/ip/dhcp/bindings`
- `GET /rci/show/ip/hotspot`
- `GET /rci/show/ip/hotspot/host`
- `GET /rci/show/ip/nat`
- `GET /rci/show/ip/route`
- `GET /rci/show/ip/service`
- `GET /rci/show/ipv6/addresses`
- `GET /rci/show/ipv6/prefixes`
- `GET /rci/show/ipv6/route`
- `GET /rci/show/ping-check`
- `GET /rci/show/ping-check?profile=`
- `GET /rci/show/rc/interface/WifiMaster1/mac/vht40`
- `GET /rci/show/rc/ip`
- `GET /rci/show/rc/ip/hotspot/auto-register`
- `GET /rci/show/rc/ip/hotspot/host`
- `GET /rci/show/rc/ip/traffic-shape`
- `GET /rci/show/rc/known/host`
- `GET /rci/show/rc/mdns/reflector`
- `GET /rci/show/rc/ping-check`
- `GET /rci/show/upnp/redirect`
- `POST /rci/ <addCommand(new>`
- `POST /rci/interface/WifiMaster1/mac/vht40`
- `POST /rci/ip/hotspot/auto-register`
- `POST /rci/ip/hotspot/wake`
- `POST /rci/show/internet/status`
- `POST /rci/show/last-change`
- `POST /rci/show/rc`
- `POST /rci/system/configuration fail-safe`
- `POST /rci/user/password/validate`
- `band`
- `ft`
- `http`
- `interface`
- `interface <addCommand(new>`
- `interface mlo`
- `ip`
- `mws wlan`
- `rc`
- `ssh`
- `telnet`

## knext/managers/deviceControl/DeviceFirmwareControlManager.java
- `<>`
- `GET /rci/show/rc/components/auto-update`
- `POST /rci/components/check-update`
- `POST /rci/show/version`

## knext/managers/deviceControl/DeviceInterfacesControlManager.java
- `<CATEGORY_SERVICE)>`
- `GET /rci/interface`
- `GET /rci/interface/authentication?name=`
- `GET /rci/interface?name=`
- `GET /rci/show/`
- `GET /rci/show/ map`
- `GET /rci/show/crypto/map/crypto_map`
- `GET /rci/show/interface/antennas?name=`
- `GET /rci/show/interface/dsl/capabilities?name=`
- `GET /rci/show/interface/ethernet/capabilities?name=`
- `GET /rci/show/rc/igmp-proxy`
- `POST /rci/interface/ipoe`
- `POST /rci/show/interface/stat`
- `crypto map <addParam(>`
- `interface`
- `interface <addCommand(new> <addNoFalseCommand(>`
- `interface <addCommand(new> <addNoTrueCommand(>`
- `speed`

## knext/managers/deviceControl/DeviceSegmentsControlManager.java
- `<>`
- `<getName)>`
- `atf`
- `auto-rescan`
- `auto-self-pin`
- `beamforming`
- `beamforming explicit`
- `compatibility`
- `country-code`
- `explicit`
- `hide-ssid`
- `interface`
- `iseg`
- `mdns reflector`
- `power`
- `security-level`
- `up`

## knext/managers/deviceControl/DeviceSystemControlManager.java
- `<>`
- `GET /ci/`
- `GET /rci/show/clock/date`
- `GET /rci/show/ndns`
- `GET /rci/show/rc/service`
- `GET /rci/show/system`
- `GET /rci/show/system/mode`
- `GET /rci/whoami`
- `POST /rci/erase`
- `POST /rci/ls`
- `POST /rci/system/log/clear`
- `POST /rci/system/reboot`
