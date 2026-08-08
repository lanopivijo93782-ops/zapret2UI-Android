package com.asterlike.zapret2ui.utils

object Constants {
    const val NOTIF_ID_VPN = 1001
    const val NOTIF_ID_PROXY = 1002
    const val CHANNEL_VPN = "zapret_vpn"
    const val CHANNEL_PROXY = "zapret_proxy"
    const val CHANNEL_DIAG = "zapret_diag"
    const val VPN_MTU = 1500
    const val VPN_ADDR = "10.111.222.1"
    const val VPN_ROUTE = "0.0.0.0"
    const val VPN_DNS_PRIMARY = "1.1.1.1"
    const val VPN_DNS_SECONDARY = "8.8.8.8"
    // DoH for Telegram proxy and ipset resolver
    const val DOH_URL = "https://1.1.1.1/dns-query"
    // GitHub API
    const val GITHUB_API_ZAPRET2 = "https://api.github.com/repos/bol-van/zapret2/releases/latest"
    const val GITHUB_API_APP = "https://api.github.com/repos/Asterlike/zapret2UI/releases/latest"
}
