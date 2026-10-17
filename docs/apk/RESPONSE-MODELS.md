# Схемы ответов из оригинального приложения (поля JSON и типы)
Источник: Gson-модели APK (@SerializedName). Это СХЕМА (имена полей, типы), не реальные значения. Статус 🔴 до подтверждения HAR/SSH.

## AccessListItem  (`com/ndmsystems/knext/models/router/AccessListItem.java`)
- `acl`: String
- `auto-delete`: Boolean
- `description`: String
- `destination-end-port`: String
- `destination-mask`: String
- `destination-port`: String
- `disable`: Boolean
- `dst-port-operator`: String
- `protocol`: String
- `schedule`: String
- `source`: String
- `source-end-port`: String
- `source-mask`: String
- `source-port`: String
- `src-port-operator`: String

## AcmeModel  (`com/ndmsystems/knext/models/show/AcmeModel.java`)
- `account-pending`: Boolean
- `account-running`: Boolean
- `apply-timer`: Integer
- `checker-timer`: Integer
- `default-domain`: String
- `default-domain-certificate-valid`: Boolean
- `get-pending`: Boolean
- `get-running`: Boolean
- `jitter`: Integer
- `ndns-domain`: String
- `ndns-domain-acme`: Boolean
- `ndns-domain-error`: Boolean
- `next-try-ta`: Integer
- `real-time`: Boolean
- `reissue-queue-size`: Integer
- `retries`: Integer
- `revoke-pending`: Boolean
- `revoke-queue-size`: Integer
- `revoke-running`: Boolean
- `server-enabled`: Boolean

## ActionModel  (`com/ndmsystems/knext/models/schedule/ActionModel.java`)
- `dow`: String
- `left`: Integer
- `next`: Boolean
- `time`: String
- `type`: String

## Address  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Address.java`)
- `dhcp`: Object
- `mask`: String

## AdjustMss  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/AdjustMss.java`)
- `pmtu`: Boolean

## Adsl  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Adsl.java`)
- `bitswap`: Boolean
- `snr-margin-offset`: Integer
- `sra`: Boolean

## AllowIp  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/AllowIp.java`)
- `mask`: String

## AntennasModel  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/AntennasModel.java`)
- `channel`: int
- `rsrp`: Integer
- `rsrq`: Integer
- `rssi`: Integer

## ArchivedConnectedDevice  (`com/ndmsystems/knext/models/connectedDevice/ArchivedConnectedDevice.java`)
- `classif`: ConnectedDeviceFromServer.Classif
- `has_random_mac`: Boolean
- `hostname`: String
- `is_track`: Boolean
- `last_available`: Integer
- `mac`: String
- `network`: String
- `registered`: Boolean

## Assign  (`com/ndmsystems/knext/models/router/dns/Assign.java`)
- `host`: Host
- `interface`: IFace

## Association  (`com/ndmsystems/knext/models/deviceControl/Association.java`)
- `ap`: String
- `authenticated`: Boolean
- `gi`: Integer
- `ht`: Integer
- `mac`: String
- `mcs`: Integer
- `mode`: String
- `rssi`: Integer
- `rxbytes`: Long
- `rxrate`: Integer
- `security`: String
- `txbytes`: Long
- `txrate`: Integer
- `txss`: Integer
- `uptime`: Integer
- `_11`: List<String>

## AuthModel  (`com/ndmsystems/knext/models/userAccount/AuthModel.java`)
- `country`: String
- `migration`: boolean
- `t`: String

## AuthTokenModel  (`com/ndmsystems/knext/models/show/dnsProxy/AuthTokenModel.java`)
- `authtoken`: String
- `comment`: String

## Authentication  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Authentication.java`)
- `chap`: Boolean
- `eap-md5`: Boolean
- `eap-mschapv2`: Boolean
- `eap-ttls`: Boolean
- `identity`: String
- `mschap`: Boolean
- `mschap-v2`: Boolean
- `pap`: Boolean
- `peap`: Boolean

## AvailableUpdateInfo  (`com/ndmsystems/knext/models/firmware/AvailableUpdateInfo.java`)
- `checking`: boolean
- `prompt`: String
- `release`: String
- `sandbox`: String
- `timestamp`: String
- `valid`: boolean

## Backhaul  (`com/ndmsystems/knext/models/show/mws/Backhaul.java`)
- `ap`: String
- `bridge`: String
- `cost`: Integer
- `duplex`: String
- `speed`: String
- `uplink`: String

## Band  (`com/ndmsystems/knext/models/connectionsInterface/profiles/modem/Band.java`)
- `band`: int
- `enabled`: boolean

## BandLists  (`com/ndmsystems/knext/models/connectionsInterface/profiles/modem/BandLists.java`)
- `lte`: List<Band>
- `nr5g`: List<Band>
- `umts`: List<Band>

## Capabilities  (`com/ndmsystems/knext/models/show/mws/Capabilities.java`)
- `backhaul-bss`: Boolean
- `dual-band`: Boolean
- `mode-hw`: Boolean
- `notify`: Boolean
- `owe`: Boolean
- `sta-mask`: Boolean
- `wpa3`: Boolean
- `wpa-eap`: Boolean

## CapabilitiesModel  (`com/ndmsystems/knext/models/show/iface/dsl/CapabilitiesModel.java`)
- `annex`: Map<String, Annex>
- `caption`: String
- `carrier`: Map<String, Carrier>
- `category`: String
- `olr`: Map<String, Olr>
- `opmode`: String
- `profile`: Map<String, Profile>
- `psdmask`: Map<String, PsdMask>
- `caption`: String
- `caption`: String
- `caption`: String
- `caption`: String
- `caption`: String

## Carrier  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Carrier.java`)
- `band`: String
- `bandwidth`: String
- `dl-freq`: Long
- `earfcn`: Integer
- `phy-cell-id`: String
- `ul-freq`: Long

## Certificate  (`com/ndmsystems/knext/models/router/ip/Certificate.java`)
- `domain`: String
- `expiration-time`: String
- `is-expired`: Boolean
- `is-ndns`: Boolean
- `should-be-renewed`: Boolean
- `issue-time`: String

## CheckNameModel  (`com/ndmsystems/knext/models/rci/ndns/CheckNameModel.java`)
- `list`: ListObj
- `acme`: Boolean
- `available`: Boolean
- `domain`: String
- `item`: List<Item>

## ClientDeviceModel  (`com/ndmsystems/knext/models/userAccount/ClientDeviceModel.java`)
- `lastaccess`: Long
- `cid`: String
- `max`: MaxClientData
- `model`: String
- `os`: String
- `telegram`: TelegramClientData
- `version`: String

## ComponentsAutoUpdate  (`com/ndmsystems/knext/models/firmware/ComponentsAutoUpdate.java`)
- `channel`: String
- `disable`: Boolean
- `schedule`: String

## Conflict  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Conflict.java`)
- `elapsed`: Integer
- `interface`: String
- `mask`: String

## Connect  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Connect.java`)
- `enabled`: Boolean
- `via`: String

## ConnectedDevice  (`com/ndmsystems/knext/models/connectedDevice/ConnectedDevice.java`)
- `hostname`: String

## ConnectedDeviceFromServer  (`com/ndmsystems/knext/models/connectedDevice/ConnectedDeviceFromServer.java`)
- `classif`: Classif
- `has_random_mac`: Boolean
- `hostname`: String
- `is_track`: Boolean
- `last_available`: Integer
- `mac`: String
- `network`: String
- `registered`: Boolean
- `group`: String
- `icon`: String
- `type`: String
- `vendor`: String

## ConnectedDeviceInRouterAdditionalMode  (`com/ndmsystems/knext/models/deviceControl/ConnectedDeviceInRouterAdditionalMode.java`)
- `ap`: String
- `ht`: Integer
- `isOnline`: boolean
- `mac`: String
- `mcs`: Integer
- `rssi`: Integer
- `txrate`: Integer
- `txss`: Integer
- `_11`: List<String>

## ContentFilter  (`com/ndmsystems/knext/models/router/internetSafety/ContentFilter.java`)
- `enable`: Boolean
- `profile`: String
- `token`: Integer
- `type`: String

## CryptoMapModel  (`com/ndmsystems/knext/models/show/crypto/map/CryptoMapModel.java`)
- `ike_state`: String

## CryptoMapStatus  (`com/ndmsystems/knext/models/show/crypto/map/CryptoMapStatus.java`)
- `l2tp_clients`: List<CryptoMapStatusProp>
- `tunnel`: List<Tunnel>
- `virtual_ip_clients`: List<CryptoMapStatusProp>
- `connect-time`: Long
- `statistic`: Statistic
- `tunnel-address`: String
- `username`: String
- `xauth-id`: String
- `rxbytes`: Long
- `txbytes`: Long
- `clientaddress`: String
- `statistic`: Statistic
- `uptime`: Long
- `username`: String

## Details  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Details.java`)
- `details`: LinkedHashMap<String, Detail>
- `last-fail`: String
- `layer`: String
- `reason`: String
- `stage`: String

## DeviceModel  (`com/ndmsystems/knext/models/userAccount/device/DeviceModel.java`)
- `interfaces`: List<DeviceInterface>
- `ip:port`: String
- `KeenDNS`: String
- `license_check`: LicenseCheck
- `ndmtitle`: String
- `wan_speed`: Integer

## DhcpHostModel  (`com/ndmsystems/knext/models/router/ip/DhcpHostModel.java`)
- `ip`: String
- `mac`: String

## DnsModel  (`com/ndmsystems/knext/models/show/dnsProxy/DnsModel.java`)
- `upstream`: List<UpstreamModel>

## DnsOverHttpsModel  (`com/ndmsystems/knext/models/router/dns/DnsOverHttpsModel.java`)
- `domain`: String
- `format`: String
- `hash`: String
- `interface`: String

## DnsOverTlsModel  (`com/ndmsystems/knext/models/router/dns/DnsOverTlsModel.java`)
- `domain`: String
- `fqdn`: String
- `hash`: String
- `interface`: String
- `port`: String

## DnsProxyFilterProfileModel  (`com/ndmsystems/knext/models/router/DnsProxyFilterProfileModel.java`)
- `description`: String
- `dns53`: Ipv4Model
- `https`: HttpsModel
- `intercept`: InterceptModel
- `tls`: TlsModel
- `upstream`: List<UpstreamUrlModel>
- `domain`: String
- `hash`: String
- `enable`: boolean
- `upstream`: List<UpstreamAddressModel>
- `upstream`: List<UpstreamAddressModel>
- `fqdn`: String
- `hash`: String
- `port`: String

## DnsProxyModel  (`com/ndmsystems/knext/models/router/dns/DnsProxyModel.java`)
- `filter`: Filter

## Dsl  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Dsl.java`)
- `ginp`: Boolean

## Event  (`com/ndmsystems/knext/models/Event.java`)
- `ts`: long
- `localized_string`: String
- `meta`: Meta
- `network`: String

## EventCount  (`com/ndmsystems/knext/models/EventCount.java`)
- `count`: int

## FamilyProfile  (`com/ndmsystems/knext/models/FamilyProfile.java`)
- `content_filter`: ContentFilter
- `dns-proxy`: FamilyProfileDnsProxy

## FamilyProfileDnsProxy  (`com/ndmsystems/knext/models/FamilyProfileDnsProxy.java`)
- `engine`: String
- `mode`: String

## Filter  (`com/ndmsystems/knext/models/router/dns/Filter.java`)
- `assign`: Assign
- `engine`: EngineType
- `profile`: HashMap<String, DnsProxyFilterProfile>
- `description`: String
- `interceptor`: static final EngineType
- `nextdns`: static final EngineType
- `safedns`: static final EngineType
- `skydns`: static final EngineType

## FirmwareUpdateStatus  (`com/ndmsystems/knext/models/firmware/updateStatus/FirmwareUpdateStatus.java`)
- `file_size`: Integer
- `progress`: Progress

## FtpModel  (`com/ndmsystems/knext/models/show/ip/FtpModel.java`)
- `enabled`: Boolean

## GetWarrantyListRes  (`com/ndmsystems/knext/models/userAccount/warranty/GetWarrantyListRes.java`)
- `activated_on`: String
- `details`: String
- `hwid`: String
- `model`: String
- `region`: String
- `registered_on`: String
- `servicetag`: String
- `token_alias`: String
- `warranty_till`: String

## Global  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Global.java`)
- `enabled`: Boolean
- `order`: Integer
- `priority`: Integer

## GumService  (`com/ndmsystems/api/gum/GumService.java`)
- `proxy`: String

## Host  (`com/ndmsystems/knext/models/router/dns/Host.java`)
- `preset`: List<HostPreset>
- `profile`: List<HostProfile>

## HostModel  (`com/ndmsystems/knext/models/show/dnsProxy/HostModel.java`)
- `preset`: List<HostPresetModel>
- `profile`: List<HostPresetModel>

## HostPreset  (`com/ndmsystems/knext/models/router/dns/HostPreset.java`)
- `host`: String
- `preset`: String

## HostPresetModel  (`com/ndmsystems/knext/models/show/dnsProxy/HostPresetModel.java`)
- `host`: String
- `preset`: String
- `profile`: String

## HostProfile  (`com/ndmsystems/knext/models/router/dns/HostProfile.java`)
- `host`: String
- `profile`: String

## HttpSslAcmeListModel  (`com/ndmsystems/knext/models/router/ip/HttpSslAcmeListModel.java`)
- `certificate`: List<Certificate>

## HttpSslModel  (`com/ndmsystems/knext/models/router/ip/HttpSslModel.java`)
- `enable`: Boolean
- `port`: String
- `redirect`: Boolean

## IFace  (`com/ndmsystems/knext/models/router/dns/IFace.java`)
- `preset`: List<InterfacePreset>
- `profile`: List<InterfaceProfile>

## IFaceStatModel  (`com/ndmsystems/knext/models/show/iface/IFaceStatModel.java`)
- `rxbytes`: Long
- `rxpackets`: Long
- `txbytes`: Long
- `txpackets`: Long

## IPSecModel  (`com/ndmsystems/knext/models/show/sc/crypto/IPSecModel.java`)
- `site-to-site`: Map<String, IPSecSiteToSiteModel>
- `autoconnect`: Boolean

## IconModel  (`com/ndmsystems/knext/models/service/account/IconModel.java`)
- `group`: String
- `icon`: String
- `type`: String

## Ike  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ike.java`)
- `server-certificate-subject`: String

## Include  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Include.java`)
- `interface`: String

## Inherit  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Inherit.java`)
- `interface`: String

## InterfaceInfo  (`com/ndmsystems/knext/models/statistic/InterfaceInfo.java`)
- `root`: Boolean
- `mac`: String

## InterfaceIpAccessGroup  (`com/ndmsystems/knext/models/router/ip/InterfaceIpAccessGroup.java`)
- `acl`: String
- `direction`: String

## InterfaceIpvcModel  (`com/ndmsystems/knext/models/interfaceRes/InterfaceIpvcModel.java`)
- `bridge`: String
- `busy-pvc`: String
- `busy-vlan`: String
- `encapsulation`: String
- `free-port`: String
- `port`: String
- `pvc-vlan`: Boolean
- `role`: String
- `vci`: String
- `vlan`: String
- `vpi`: String

## InterfaceModel  (`com/ndmsystems/knext/models/show/dnsProxy/InterfaceModel.java`)
- `preset`: List<InterfacePresetModel>
- `profile`: List<InterfacePresetModel>

## InterfacePreset  (`com/ndmsystems/knext/models/router/dns/InterfacePreset.java`)
- `interface`: String
- `preset`: String

## InterfacePresetModel  (`com/ndmsystems/knext/models/show/dnsProxy/InterfacePresetModel.java`)
- `interface`: String
- `preset`: String
- `profile`: String

## InterfaceProfile  (`com/ndmsystems/knext/models/router/dns/InterfaceProfile.java`)
- `interface`: String
- `profile`: String

## InterfaceRxTxData  (`com/ndmsystems/knext/models/statistic/InterfaceRxTxData.java`)
- `iface`: InterfaceInfo
- `traffic`: List<RxTxData>

## InternetStatus  (`com/ndmsystems/knext/models/router/InternetStatus.java`)
- `checked`: String
- `dns-accessible`: Boolean
- `enabled`: Boolean
- `gateway-accessible`: Boolean
- `host-accessible`: Boolean
- `internet`: Boolean
- `reliable`: Boolean

## Ip  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ip.java`)
- `adjust-ttl`: AdjustTtl
- `dhcp`: Dhcp
- `gateway`: String
- `global`: Global
- `mtu`: String
- `name-server`: List<NameServer>
- `name-servers`: Boolean
- `remote`: Remote
- `tcp`: Tcp
- `recv`: String
- `send`: String
- `client`: Client
- `hostname`: String
- `name-servers`: Boolean
- `enabled`: Boolean
- `order`: Integer
- `priority`: Integer

## IpArp  (`com/ndmsystems/knext/models/router/ip/IpArp.java`)
- `interface`: String
- `ip`: String
- `mac`: String

## IpHotspotHost  (`com/ndmsystems/knext/models/router/ip/IpHotspotHost.java`)
- `dns-filter`: IpHotspotHostDnsFilter
- `hostname`: String
- `ip`: String
- `mac`: String
- `policy`: String

## IpHotspotHostDnsFilter  (`com/ndmsystems/knext/models/router/ip/IpHotspotHostDnsFilter.java`)
- `engine`: String
- `profile`: String

## IpRouteModel  (`com/ndmsystems/knext/models/router/ip/IpRouteModel.java`)
- `comment`: String
- `reject`: Boolean
- `gateway`: String
- `host`: String
- `interface`: String
- `mask`: String
- `network`: String

## IpStaticModel  (`com/ndmsystems/knext/models/router/ip/IpStaticModel.java`)
- `bytes`: Integer
- `comment`: String
- `description`: String
- `disable`: Boolean
- `end-port`: String
- `interface`: String
- `mask`: String
- `packets`: Integer
- `port`: String
- `protocol`: String
- `schedule`: String
- `to-address`: String
- `to-host`: String
- `to-interface`: String
- `to-port`: String

## IpTrafficShapeUnknownHostModel  (`com/ndmsystems/knext/models/router/ip/IpTrafficShapeUnknownHostModel.java`)
- `rate`: Long
- `upstream-rate`: Long

## Ipoe  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ipoe.java`)
- `busy-vlan`: String
- `free-port`: String
- `inet-port`: String
- `inet-vlan`: String
- `iptv-port`: String
- `iptv-vlan`: String
- `voip-port`: String
- `voip-vlan`: String

## Ipsec  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ipsec.java`)
- `name-servers`: Boolean
- `preshared-key`: PresharedKey

## Ipv6  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ipv6.java`)
- `block`: String
- `name-server`: String
- `port`: String
- `prefix`: String

## Ipv6Show  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Ipv6Show.java`)
- `addresses`: List<Addresses>
- `defaultgw`: Boolean
- `prefix-length`: Integer
- `proto`: String
- `valid-lifetime`: String

## Iseg  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Iseg.java`)
- `busy-vlan`: String
- `free-port`: String
- `include`: String
- `port`: String
- `vlan`: String
- `vlan-port`: String

## KnownHostInfo  (`com/ndmsystems/knext/models/deviceControl/KnownHostInfo.java`)
- `interface`: Interface
- `access`: String
- `ap`: String
- `dhcp`: Dhcp
- `dns-filter`: DnsFilter
- `firstSeen`: Integer
- `hostname`: String
- `ht`: Integer
- `ip`: String
- `isContainsMws`: Boolean
- `lastSeen`: Integer
- `link`: String
- `mac`: String
- `mcs`: Integer
- `mld`: List<Mld>
- `mode`: String
- `mws`: Mws
- `priority`: Integer
- `registered`: boolean
- `rssi`: Integer
- `rxbytes`: Long
- `schedule`: String
- `speed`: Integer
- `ssid`: String
- `system-mode`: String
- `traffic-shape`: TrafficShape
- `txbytes`: Long
- `txrate`: Integer
- `txss`: Integer
- `uptime`: Integer
- `via`: String
- `mac-access`: HashMap<String, String>
- `_11`: List<String>

## L2tpServer  (`com/ndmsystems/knext/models/show/rc/crypto/L2tpServer.java`)
- `static-ip`: Map<String, VirtualIpStaticIp>

## L2tpServerModel  (`com/ndmsystems/knext/models/show/rc/crypto/L2tpServerModel.java`)
- `enable`: Boolean
- `interface`: String
- `multi-login`: Boolean
- `nat`: Boolean
- `pool-size`: String
- `pool-start`: String
- `psk`: String
- `sa-compat`: String

## LinkAggregation  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/LinkAggregation.java`)
- `enabled`: Boolean
- `group-id`: Integer
- `master`: Boolean
- `supported`: Boolean

## LockoutPolicyModel  (`com/ndmsystems/knext/models/router/ip/rc/LockoutPolicyModel.java`)
- `observation-window`: String
- `threshold`: String

## LsEntryModel  (`com/ndmsystems/knext/models/router/LsEntryModel.java`)
- `mounted`: String
- `size`: String
- `type`: String
- `uuid`: String

## LsModel  (`com/ndmsystems/knext/models/router/LsModel.java`)
- `entry`: Map<String, LsEntryModel>
- `rel`: String

## Mac  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Mac.java`)
- `bssid`: String
- `mac`: String

## MapVirtualIpServerIKEv2Model  (`com/ndmsystems/knext/models/show/rc/crypto/MapVirtualIpServerIKEv2Model.java`)
- `enable`: Boolean
- `match-address`: MatchAddress
- `set-peer`: SetPeer
- `set-profile`: SetProfile
- `set-transform`: SetTransform
- `virtual-ip`: VirtualIp

## MapVirtualIpServerModel  (`com/ndmsystems/knext/models/show/rc/crypto/MapVirtualIpServerModel.java`)
- `enable`: Boolean
- `match-address`: MatchAddress
- `set-peer`: SetPeer
- `set-profile`: SetProfile
- `set-transform`: SetTransform
- `virtual-ip`: VirtualIp

## MapVpnL2tpServerModel  (`com/ndmsystems/knext/models/show/rc/crypto/MapVpnL2tpServerModel.java`)
- `enable`: Boolean
- `l2tp-server`: L2tpServer
- `match-address`: MatchAddress
- `set-peer`: SetPeer
- `set-profile`: SetProfile
- `set-transform`: SetTransform

## MatchAddress  (`com/ndmsystems/knext/models/show/rc/crypto/MatchAddress.java`)
- `access-list`: String

## MaxClientData  (`com/ndmsystems/knext/models/userAccount/MaxClientData.java`)
- `bot`: String
- `chatid`: String
- `code`: String

## Meta  (`com/ndmsystems/knext/models/Meta.java`)
- `dynamic_link`: String

## Mld  (`com/ndmsystems/knext/models/deviceControl/Mld.java`)
- `ap`: String
- `authenticated`: Boolean
- `ebf`: Boolean
- `gi`: Integer
- `ht`: Integer
- `mcs`: Integer
- `mld`: Boolean
- `mode`: String
- `pmf`: Boolean
- `psm`: Boolean
- `rssi`: Integer
- `security`: String
- `txrate`: Integer
- `txss`: Integer
- `uptime`: Integer
- `_11`: List<String>

## Mobile  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Mobile.java`)
- `pdp`: String

## Mws  (`com/ndmsystems/knext/models/deviceControl/Mws.java`)
- `ap`: String
- `authenticated`: Boolean
- `cid`: String
- `dl-mu`: Boolean
- `ebf`: Boolean
- `gi`: Integer
- `ht`: Integer
- `mcs`: Integer
- `mode`: String
- `rssi`: Integer
- `security`: String
- `txrate`: Integer
- `txss`: Integer
- `uptime`: Integer
- `_11`: List<String>

## MwsItemModel  (`com/ndmsystems/knext/models/show/mws/MwsItemModel.java`)
- `associations`: Integer
- `backhaul`: Backhaul
- `capabilities`: Capabilities
- `cid`: String
- `fw`: String
- `fw-available`: String
- `ip`: String
- `known-host`: String
- `license`: String
- `mac`: String
- `mode`: String
- `model`: String
- `rci`: Rci

## MwsWlanRcStatus  (`com/ndmsystems/knext/models/mwsWlan/MwsWlanRcStatus.java`)
- `band`: List<String>
- `band-steering`: WlanBandSteering
- `bind`: WlanBind
- `description`: String
- `enable`: Boolean
- `encryption`: String
- `ft`: WlanFt
- `mlo`: WlanMlo
- `peer-isolation`: Boolean
- `rrm`: WlanRrm
- `schedule`: String
- `ssid`: WlanSsid
- `wpa`: WlanWpa
- `wps`: WlanWps
- `enable`: Boolean
- `prefer-band`: String
- `interface`: String
- `enable`: Boolean
- `iapp`: WlanFtIapp
- `mdid`: String
- `skip-band`: List<String>
- `key`: String
- `enable`: Boolean
- `skip-band`: List<String>
- `enable`: Boolean
- `skip-band`: List<String>
- `hide`: Boolean
- `eap`: WlanWpaEap
- `psk`: String
- `authentication`: String
- `radius`: WlanWpaEapRadius
- `secret`: String
- `server`: WlanWpaEapRadiusServer
- `port`: String
- `enable`: Boolean

## NameServer  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/NameServer.java`)
- `name-server`: String

## NdnsModel  (`com/ndmsystems/knext/models/show/NdnsModel.java`)
- `access`: String
- `access6`: String
- `address6`: String
- `booked`: String
- `domain`: String
- `ttp`: Ttp
- `updated`: Boolean
- `xns`: String

## NetworkModel  (`com/ndmsystems/knext/models/userAccount/NetworkModel.java`)
- `devices`: ArrayList<DeviceModel>
- `short_devices`: ArrayList<NetworkShortDeviceModel>

## NetworkShortDeviceModel  (`com/ndmsystems/knext/models/userAccount/device/NetworkShortDeviceModel.java`)
- `is_invalid_pass`: Boolean
- `is_unauthorized_access`: Boolean
- `password_status`: String

## NetworksManager  (`com/ndmsystems/knext/managers/account/NetworksManager.java`)
- `has_next_page`: boolean

## NewRegionData  (`com/ndmsystems/knext/models/regions/NewRegionData.java`)
- `default_locale`: String
- `legal_text`: String
- `code`: String

## NotificationsEventModel  (`com/ndmsystems/knext/ui/refactored/events/subscreen/notifications/model/NotificationsEventModel.java`)
- `is_email`: boolean
- `is_max`: boolean
- `is_push`: boolean
- `is_telegram`: boolean

## NtceGroupSetModel  (`com/ndmsystems/knext/models/show/rc/NtceGroupSetModel.java`)
- `groupset`: List<Groupset>
- `id-num`: Integer
- `long`: String
- `short`: String

## NtceGroupsModel  (`com/ndmsystems/knext/models/show/rc/NtceGroupsModel.java`)
- `group`: List<Group>
- `groupset-id`: Integer
- `groupset-long-id`: String
- `groupset-short-id`: String
- `id-num`: Integer
- `long`: String

## NtceQoSModel  (`com/ndmsystems/knext/models/show/rc/NtceQoSModel.java`)
- `category`: List<Category>
- `enable`: Boolean
- `category`: String
- `priority`: Integer

## OCServerModel  (`com/ndmsystems/knext/models/show/rc/OCServerModel.java`)
- `config`: Config
- `interface`: String
- `pool-range`: PoolRange
- `camouflage`: Boolean
- `enable`: Boolean
- `interface`: String
- `multi-login`: Boolean
- `nat`: Boolean
- `pool-size`: String
- `pool-start`: String
- `session-preempt`: Boolean
- `users`: Map<String, User>
- `access`: Boolean
- `begin`: String
- `size`: String

## OpenConnect  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/OpenConnect.java`)
- `connect`: Connect
- `upstream`: Upstream
- `via`: String
- `host`: String
- `port`: String

## OpenVpn  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/OpenVpn.java`)
- `connect`: Connect
- `via`: String

## OperatingMode  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/OperatingMode.java`)
- `annex`: String
- `mode`: String

## OperatorModel  (`com/ndmsystems/knext/models/show/iface/OperatorModel.java`)
- `mobile`: String
- `plmn`: String

## PasswordLength  (`com/ndmsystems/knext/models/deviceControl/PasswordLength.java`)
- `recommended`: int
- `required`: int

## PasswordLowStateReason  (`com/ndmsystems/knext/models/deviceControl/PasswordLowStateReason.java`)
- `too-short`: static final PasswordLowStateReason
- `blocklist`: static final PasswordLowStateReason
- `user-name`: static final PasswordLowStateReason
- `format`: static final PasswordLowStateReason

## PasswordStrength  (`com/ndmsystems/knext/models/deviceControl/PasswordStrength.java`)
- `low`: static final PasswordStrength
- `fair`: static final PasswordStrength
- `good`: static final PasswordStrength

## PasswordValidationResult  (`com/ndmsystems/knext/models/deviceControl/PasswordValidationResult.java`)
- `length`: PasswordLength
- `reason`: String
- `strength`: String

## PeerInfo  (`com/ndmsystems/api/models/PeerInfo.java`)
- `cid`: String
- `cloud`: boolean
- `eula`: boolean
- `http_port`: int
- `type`: String
- `version`: String

## PeerModel  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/PeerModel.java`)
- `comment`: String
- `last-handshake`: Long
- `local-port`: Integer
- `public-key`: String
- `remote`: String
- `remote-port`: Integer
- `rxbytes`: Long
- `txbytes`: Long
- `via`: String

## PingCheck  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/PingCheck.java`)
- `profile`: String

## Plan  (`com/ndmsystems/knext/models/show/dnsProxy/Plan.java`)
- `code`: String

## PoolRange  (`com/ndmsystems/knext/models/show/PoolRange.java`)
- `begin`: String
- `size`: String

## PptpVpnServerModel  (`com/ndmsystems/knext/models/show/rc/PptpVpnServerModel.java`)
- `interface`: String
- `mppe-optional`: boolean
- `multi-login`: boolean
- `pool-range`: PoolRange
- `static-ip`: Map<String, VirtualIpStaticIp>

## PresetsModel  (`com/ndmsystems/knext/models/show/dnsProxy/PresetsModel.java`)
- `description`: String
- `id`: String
- `short-description`: String
- `stale`: Boolean

## PresharedKey  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/PresharedKey.java`)
- `key`: String

## ProfileModel  (`com/ndmsystems/knext/models/show/dnsProxy/ProfileModel.java`)
- `description`: String
- `dns53`: DnsModel

## Progress  (`com/ndmsystems/knext/models/firmware/updateStatus/Progress.java`)
- `Components::Manager`: String
- `flash:firmware`: String

## Proxy  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Proxy.java`)
- `connect`: ConnectVia
- `protocol`: Protocol
- `upstream`: Upstream
- `via`: String
- `proto`: String
- `host`: String
- `port`: String

## RcFtpModel  (`com/ndmsystems/knext/models/show/rc/ip/RcFtpModel.java`)
- `enable`: Boolean
- `permissive`: Boolean
- `port`: String
- `root`: Root
- `security-level`: SecurityLevel

## RcIpHotspotHostModel  (`com/ndmsystems/knext/models/router/ip/RcIpHotspotHostModel.java`)
- `access`: HotspotPolicy
- `conform`: Boolean
- `mac`: String
- `policy`: String

## RcPingcheckModel  (`com/ndmsystems/knext/models/show/rc/RcPingcheckModel.java`)
- `profile`: Map<String, Profile>
- `host`: List<String>
- `max-fails`: MaxFails
- `mode`: String
- `port`: Integer
- `update-interval`: UpdateInterval
- `count`: Integer
- `seconds`: Integer

## RcServiceModel  (`com/ndmsystems/knext/models/show/rc/RcServiceModel.java`)
- `afp`: Boolean
- `cifs`: Boolean
- `ipsec`: Boolean
- `ntce`: Boolean
- `oc-server`: Boolean
- `ssh`: Boolean
- `udpxy`: Boolean
- `vpn-server`: Boolean

## RcSshModel  (`com/ndmsystems/knext/models/show/rc/ip/RcSshModel.java`)
- `port`: String
- `security-level`: SecurityLevel
- `sftp`: RcSshSftpModel

## RcSshSftpModel  (`com/ndmsystems/knext/models/show/rc/ip/RcSshSftpModel.java`)
- `enable`: Boolean
- `permissive`: Boolean
- `root`: Root

## Rci  (`com/ndmsystems/knext/models/show/mws/Rci.java`)
- `errors`: Integer

## RegisterRes  (`com/ndmsystems/knext/models/userAccount/warranty/RegisterRes.java`)
- `details`: String
- `register`: Boolean
- `warranty_till`: String

## ReleaseNotesModel  (`com/ndmsystems/knext/models/firmware/ReleaseNotesModel.java`)
- `date`: String
- `href`: String

## RemoteAccessIpModel  (`com/ndmsystems/knext/models/router/ip/rc/RemoteAccessIpModel.java`)
- `port`: int
- `security-level`: SecurityLevelModel
- `ssl`: SslModel

## ResponseRciStatus  (`com/ndmsystems/knext/models/rci/ResponseRciStatus.java`)
- `code`: String
- `ident`: String
- `message`: String
- `source`: String
- `warning`: String

## RoleShow  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/RoleShow.java`)
- `for`: String
- `role`: String

## Root  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Root.java`)
- `directory`: String

## RxTxCdData  (`com/ndmsystems/knext/models/statistic/RxTxCdData.java`)
- `mac`: String
- `rx`: long
- `tx`: long

## RxTxData  (`com/ndmsystems/knext/models/statistic/RxTxData.java`)
- `rx`: long
- `ts`: long
- `tx`: long

## RxTxFpData  (`com/ndmsystems/knext/models/statistic/RxTxFpData.java`)
- `rx`: long
- `tx`: long

## ScCryptoMapModel  (`com/ndmsystems/knext/models/show/sc/crypto/ScCryptoMapModel.java`)
- `map`: Map<String, IPSecSiteToSiteModel>
- `enable`: Boolean

## SecurityLevel  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/SecurityLevel.java`)
- `private`: Boolean

## SecurityLevelModel  (`com/ndmsystems/knext/models/router/ip/rc/SecurityLevelModel.java`)
- `private`: Boolean
- `ssl`: Boolean

## ServiceModel  (`com/ndmsystems/knext/models/show/ip/ServiceModel.java`)
- `family`: String
- `port`: Integer
- `protocol`: String
- `service-name`: String

## SetPeer  (`com/ndmsystems/knext/models/show/rc/crypto/SetPeer.java`)
- `remote-ip`: String

## SetProfile  (`com/ndmsystems/knext/models/show/rc/crypto/SetProfile.java`)
- `profile`: String

## SetTransform  (`com/ndmsystems/knext/models/show/rc/crypto/SetTransform.java`)
- `transform-set`: String

## ShareModel  (`com/ndmsystems/knext/models/show/ShareModel.java`)
- `description`: String
- `mount`: String
- `timemachine`: Boolean

## ShowAfpModel  (`com/ndmsystems/knext/models/show/ShowAfpModel.java`)
- `automount`: boolean
- `enabled`: boolean
- `permissive`: boolean

## ShowCifsModel  (`com/ndmsystems/knext/models/show/ShowCifsModel.java`)
- `automount`: boolean
- `enabled`: boolean
- `map-hidden`: boolean
- `permissive`: boolean

## ShowClockModel  (`com/ndmsystems/knext/models/show/ShowClockModel.java`)
- `day`: Integer
- `hour`: Integer
- `min`: Integer
- `month`: Integer
- `sec`: Integer
- `year`: Integer

## ShowDnsProxyFilterAssignModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowDnsProxyFilterAssignModel.java`)
- `host`: HostModel
- `interface`: InterfaceModel

## ShowDnsProxyFilterPresets  (`com/ndmsystems/knext/models/show/dnsProxy/ShowDnsProxyFilterPresets.java`)
- `presets`: Map<String, PresetsModel>
- `version`: Integer

## ShowDnsProxyFilterProfileModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowDnsProxyFilterProfileModel.java`)
- `profile`: Map<String, ProfileModel>

## ShowDnsUserInfoModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowDnsUserInfoModel.java`)
- `plan`: Plan

## ShowEulaDpnDocumentModel  (`com/ndmsystems/knext/models/show/ShowEulaDpnDocumentModel.java`)
- `format`: String
- `language`: String
- `text`: String
- `version`: Long

## ShowEulaDpnListModel  (`com/ndmsystems/knext/models/show/ShowEulaDpnListModel.java`)
- `document`: List<Document>
- `version`: Long
- `format`: List<String>
- `lang`: String

## ShowIFaceDslDiagnosticsModel  (`com/ndmsystems/knext/models/show/iface/dsl/ShowIFaceDslDiagnosticsModel.java`)
- `device`: String
- `file`: String
- `finished`: String
- `left`: Long

## ShowIFaceDslDisconnectReportModel  (`com/ndmsystems/knext/models/show/iface/dsl/ShowIFaceDslDisconnectReportModel.java`)
- `device`: String
- `file`: String
- `finished`: String
- `upload`: Upload
- `file`: String
- `time`: String

## ShowIFaceOperatorsModel  (`com/ndmsystems/knext/models/show/iface/ShowIFaceOperatorsModel.java`)
- `age`: Integer
- `operator`: List<OperatorModel>
- `scanning`: String

## ShowIpRouteModel  (`com/ndmsystems/knext/models/router/ip/ShowIpRouteModel.java`)
- `floating`: Boolean
- `gateway`: String
- `interface`: String
- `metric`: Integer
- `proto`: String

## ShowIpv6AddressesModel  (`com/ndmsystems/knext/models/router/ip/ShowIpv6AddressesModel.java`)
- `interface`: String
- `proto`: String
- `valid-lifetime`: String

## ShowIpv6PrefixesModel  (`com/ndmsystems/knext/models/router/ip/ShowIpv6PrefixesModel.java`)
- `global`: Boolean
- `interface`: String
- `preferred-lifetime`: String
- `prefix`: String
- `valid-lifetime`: String

## ShowIpv6RouteModel  (`com/ndmsystems/knext/models/router/ip/ShowIpv6RouteModel.java`)
- `floating`: Boolean
- `gateway`: String
- `interface`: String
- `metric`: Integer
- `proto`: String

## ShowLastChangeModel  (`com/ndmsystems/knext/models/show/ShowLastChangeModel.java`)
- `agent`: String
- `checksum`: String
- `date`: String
- `dpn-status`: DpnStatus
- `easyconfig`: Boolean
- `eula-status`: EulaStatus
- `fail-safe`: FailSafe
- `user`: String
- `accepted`: Long
- `select-country`: Boolean
- `version`: Long
- `version`: Long
- `blocked`: Boolean
- `rollback`: Boolean
- `time`: Long
- `unsaved`: Boolean

## ShowMwsControllerStatus  (`com/ndmsystems/knext/models/mwsWlan/ShowMwsControllerStatus.java`)
- `backhaul`: Backhaul
- `cid`: String
- `ip`: String
- `mac`: String
- `port`: Integer
- `region`: String
- `ta-fqdn`: String
- `auto-negotiation`: String
- `bridge`: String
- `bss-shutdown`: Boolean
- `cost`: Integer
- `duplex`: String
- `eee`: String
- `flow-control`: String
- `link`: String
- `mac`: String
- `port-label`: String
- `root`: String
- `speed`: String
- `uplink`: String

## ShowMwsWlanStatus  (`com/ndmsystems/knext/models/mwsWlan/ShowMwsWlanStatus.java`)
- `band`: Map<String, WlanBandStatus>
- `bind`: WlanBind
- `disabled-by-schedule`: Boolean
- `access-point`: Map<String, AccessPoint>
- `enabled`: Boolean
- `wps`: Wps
- `configured`: Boolean
- `interface`: String

## ShowNextDnsProfilesModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowNextDnsProfilesModel.java`)
- `profile`: Map<String, ShowNextProfileModel>

## ShowNextProfileModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowNextProfileModel.java`)
- `token`: String

## ShowOCServerModel  (`com/ndmsystems/knext/models/show/ShowOCServerModel.java`)
- `fqdn`: String
- `has-ndns-certificate`: String
- `ndns-name`: String
- `secret`: String
- `tunnel`: List<ShowSstpServerModel.Tunnel>

## ShowPingcheckProfileModel  (`com/ndmsystems/knext/models/show/ShowPingcheckProfileModel.java`)
- `host`: List<String>
- `interface`: HashMap<String, PingcheckInterface>
- `max-fails`: Integer
- `mode`: String
- `profile`: String
- `update-interval`: Integer
- `failcount`: Integer
- `successcount`: Integer

## ShowRcIpHotspotAutoregisterDisableModel  (`com/ndmsystems/knext/models/show/ShowRcIpHotspotAutoregisterDisableModel.java`)
- `disable`: Boolean

## ShowRcIpHttpModel  (`com/ndmsystems/knext/models/show/rc/ip/http/ShowRcIpHttpModel.java`)
- `security-level`: SecurityLevel
- `ssl`: HttpSslModel
- `webdav`: WebdavModel
- `private`: Boolean
- `ssl`: Boolean

## ShowRcIpv6NameServerModel  (`com/ndmsystems/knext/models/router/ip/ShowRcIpv6NameServerModel.java`)
- `domain`: String
- `interface`: String

## ShowRcMdnsReflectorModel  (`com/ndmsystems/knext/models/show/ShowRcMdnsReflectorModel.java`)
- `disable`: Boolean
- `enforce`: Boolean

## ShowRcNextDnsAssignModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcNextDnsAssignModel.java`)
- `host`: String
- `iface`: String
- `token`: String

## ShowRcNextDnsModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcNextDnsModel.java`)
- `assign`: List<ShowRcNextDnsAssignModel>
- `authtoken`: AuthTokenModel

## ShowRcSafeDnsAssignModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcSafeDnsAssignModel.java`)
- `host`: String
- `iface`: String
- `token`: Integer

## ShowRcSafeDnsModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcSafeDnsModel.java`)
- `assign`: List<ShowRcSafeDnsAssignModel>
- `login`: String

## ShowRcSkyDnsAssignModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcSkyDnsAssignModel.java`)
- `host`: String
- `iface`: String
- `token`: Integer

## ShowRcSkyDnsModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowRcSkyDnsModel.java`)
- `assign`: List<ShowRcSkyDnsAssignModel>
- `login`: String

## ShowSafeDnsProfilesModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowSafeDnsProfilesModel.java`)
- `profile`: Map<String, ShowSafeProfileModel>

## ShowSafeProfileModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowSafeProfileModel.java`)
- `token`: Integer

## ShowSkyDnsProfilesModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowSkyDnsProfilesModel.java`)
- `profile`: Map<String, ShowSkyProfileModel>

## ShowSkyProfileModel  (`com/ndmsystems/knext/models/show/dnsProxy/ShowSkyProfileModel.java`)
- `token`: Integer

## ShowSstpServerModel  (`com/ndmsystems/knext/models/show/ShowSstpServerModel.java`)
- `enabled`: Boolean
- `has-ndns-certificate`: Boolean
- `ndns-name`: String
- `tunnel`: List<Tunnel>
- `rxbytes`: Long
- `txbytes`: Long
- `clientaddress`: String
- `statistic`: Statistic
- `uptime`: Long
- `username`: String

## ShowTagsModel  (`com/ndmsystems/knext/models/show/ShowTagsModel.java`)
- `tag`: List<String>

## ShowVpnServer  (`com/ndmsystems/knext/models/show/ShowVpnServer.java`)
- `tunnel`: List<Tunnel>
- `rxbytes`: Long
- `txbytes`: Long
- `clientaddress`: String
- `statistic`: Statistic
- `uptime`: Long
- `username`: String

## SimpleConnectionPolicy  (`com/ndmsystems/knext/models/connectedDevice/SimpleConnectionPolicy.java`)
- `description`: String

## SiteSurveyModel  (`com/ndmsystems/knext/models/show/SiteSurveyModel.java`)
- `channel`: Integer
- `encryption`: String
- `ieee`: String
- `quality`: Integer
- `rssi`: Integer
- `essid`: String

## SmsListModel  (`com/ndmsystems/knext/models/sms/SmsListModel.java`)
- `messages`: Map<String, Message>
- `messages-count`: Integer
- `nv-free-slots`: Integer
- `nv-total-slots`: Integer
- `sim-free-slots`: Integer
- `sim-total-slots`: Integer
- `from`: String
- `parts`: Integer
- `read`: Boolean
- `text`: String
- `timestamp`: String
- `total-parts`: Integer

## SslModel  (`com/ndmsystems/knext/models/router/ip/rc/SslModel.java`)
- `enable`: Boolean
- `port`: String
- `redirect`: Boolean

## SstpServerModel  (`com/ndmsystems/knext/models/show/SstpServerModel.java`)
- `interface`: String
- `multi-login`: boolean
- `pool-range`: PoolRange
- `static-ip`: Map<String, VirtualIpStaticIp>

## Standby  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Standby.java`)
- `enable`: Boolean
- `timeout`: Timeout
- `seconds`: Long

## Status  (`com/ndmsystems/knext/models/firmware/updateStatus/Status.java`)
- `code`: String
- `ident`: String
- `message`: String
- `source`: String
- `warning`: String

## Summary  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Summary.java`)
- `layer`: LinkedHashMap<String, String>

## SystemDeviceInfo  (`com/ndmsystems/knext/models/deviceControl/SystemDeviceInfo.java`)
- `cpuload`: Integer
- `domainname`: String
- `hostname`: String
- `memfree`: Integer
- `memtotal`: Integer
- `uptime`: Long

## SystemModel  (`com/ndmsystems/knext/models/show/mws/SystemModel.java`)
- `cpuload`: Integer
- `memory`: String
- `uptime`: String

## Tcp  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Tcp.java`)
- `adjust-mss`: AdjustMss

## TelegramClientData  (`com/ndmsystems/knext/models/userAccount/TelegramClientData.java`)
- `bot`: String
- `chatid`: String
- `code`: String

## TrafficShape  (`com/ndmsystems/knext/models/connectedDevice/TrafficShape.java`)
- `mode`: String
- `rx`: long
- `schedule`: String
- `tx`: long

## TrafficShapeTemp  (`com/ndmsystems/knext/models/connectedDevice/TrafficShapeTemp.java`)
- `mac`: String
- `upstream-rate`: long
- `schedule`: String
- `rate`: long

## Ttp  (`com/ndmsystems/knext/models/show/Ttp.java`)
- `direct`: Boolean
- `interface`: String

## Tunnel  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Tunnel.java`)
- `source`: Source

## UdpxyModel  (`com/ndmsystems/knext/models/show/rc/UdpxyModel.java`)
- `interface`: String
- `port`: Integer
- `renew-interval`: Integer
- `timeout`: Integer

## UnassignedDevice  (`com/ndmsystems/knext/models/UnassignedDevice.java`)
- `classif`: ConnectedDeviceFromServer.Classif
- `mac`: String

## UnregisterWarrantyRes  (`com/ndmsystems/knext/models/userAccount/warranty/UnregisterWarrantyRes.java`)
- `unregister`: Boolean

## UsbDeviceModel  (`com/ndmsystems/knext/models/usb/UsbDeviceModel.java`)
- `DEVPATH`: String
- `DEVICE`: String
- `manufacturer`: String
- `port`: String
- `power-control`: String
- `product`: String
- `serial`: String
- `subsystem`: String
- `usb-version`: String

## UsbMediaModel  (`com/ndmsystems/knext/models/usb/UsbMediaModel.java`)
- `ejectable`: Boolean
- `manufacturer`: String
- `product`: String
- `serial`: String
- `size`: String
- `port`: int
- `partition`: List<UsbMediaPartitionModel>

## UsbMediaPartitionModel  (`com/ndmsystems/knext/models/usb/UsbMediaPartitionModel.java`)
- `free`: String
- `fstype`: String
- `total`: String
- `uuid`: String

## Vdsl  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Vdsl.java`)
- `enabled`: Boolean
- `non-std`: Boolean
- `mask`: String

## VirtualIpMultiLogin  (`com/ndmsystems/knext/models/show/rc/crypto/VirtualIpMultiLogin.java`)
- `enable`: Boolean

## VirtualIpNat  (`com/ndmsystems/knext/models/show/rc/crypto/VirtualIpNat.java`)
- `enable`: Boolean

## VirtualIpRange  (`com/ndmsystems/knext/models/show/rc/crypto/VirtualIpRange.java`)
- `begin`: String
- `end`: String

## VirtualIpServerIKEv2Model  (`com/ndmsystems/knext/models/show/rc/crypto/VirtualIpServerIKEv2Model.java`)
- `dns-server`: String
- `enable`: Boolean
- `multi-login`: Boolean
- `nat`: Boolean
- `pool-size`: String
- `pool-start`: String
- `sa-compat`: String

## VirtualIpServerModel  (`com/ndmsystems/knext/models/show/rc/crypto/VirtualIpServerModel.java`)
- `dns-server`: String
- `enable`: Boolean
- `nat`: Boolean
- `pool-size`: String
- `pool-start`: String
- `psk`: String
- `sa-compat`: String

## WebdavModel  (`com/ndmsystems/knext/models/show/rc/ip/http/WebdavModel.java`)
- `enable`: Boolean
- `permissive`: Boolean
- `root`: Root
- `security-level`: SecurityLevel

## WhoamiInfo  (`com/ndmsystems/knext/models/deviceControl/WhoamiInfo.java`)
- `agent`: String
- `demo-mode`: boolean
- `host`: String
- `mac`: String
- `manager`: boolean
- `readonly`: boolean
- `user`: String
- `where`: String

## WifiNetworkInfo  (`com/ndmsystems/knext/models/connectionsInterface/WifiNetworkInfo.java`)
- `ext-40-above`: boolean
- `ext-40-below`: boolean
- `number`: int
- `vht-80`: boolean
- `wave2-160`: boolean
- `wave2-80-80`: boolean

## Wireguard  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/Wireguard.java`)
- `listen-port`: Integer
- `peer`: ArrayList<PeerModel>
- `public-key`: String
- `server-owned`: Boolean

## WireguardRc  (`com/ndmsystems/knext/models/deviceControl/oneInteraceParams/WireguardRc.java`)
- `listen-port`: ListenPort
- `port`: Integer

## WireguardServerCreatePeersResponse  (`com/ndmsystems/knext/models/show/rc/WireguardServerCreatePeersResponse.java`)
- `config`: Config
- `peers`: Peers
- `id`: String
- `new-peer-config`: String
- `create`: List<CreatedPeer>

## WireguardServerModel  (`com/ndmsystems/knext/models/show/rc/WireguardServerModel.java`)
- `config`: Config
- `enable`: Boolean
- `interface`: String
- `nat`: Boolean
- `peers`: Map<String, Peer>
- `subnet`: String
- `access`: Boolean
- `allow-ips`: String
- `description`: String

## WpaEap  (`com/ndmsystems/knext/models/deviceControl/segmentInfo/WpaEap.java`)
- `radius`: WpaEapRadius

## WpaEapRadius  (`com/ndmsystems/knext/models/deviceControl/segmentInfo/WpaEapRadius.java`)
- `secret`: String
- `server`: WpaEapRadiusServer

## WpaEapRadiusServer  (`com/ndmsystems/knext/models/deviceControl/segmentInfo/WpaEapRadiusServer.java`)
- `port`: String

## WpsStatus  (`com/ndmsystems/knext/models/wps/WpsStatus.java`)
- `auto-self-pin`: Boolean
- `configured`: Boolean
- `direction`: String
- `left`: Integer
- `mode`: String
- `owe`: Boolean
- `wpa3-only`: Boolean
