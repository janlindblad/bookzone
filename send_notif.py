"""
*********************************************************************
* YANG Book Send Purchase Notification                              *
* Derived from: ConfD Actions intro example                         *
*                                                                   *
* (C) 2018 Jan Lindblad                                             *
*                                                                   *
* See the LICENSE file for license information                      *
* See the README  file for more information                         *
*********************************************************************
"""
import time
import confd
from confd.dp import Daemon
from confd.maapi import Maapi
import _confd.dp as lowdp
from bookzone_example_ns import ns

def send_shipping_notif(stream, user, title, fmt, copies):
  values = [
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_shipping), confd.Value((ns.bz_shipping, ns.hash), confd.C_XMLBEGIN)),
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_user),     confd.Value(user)),
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_title),    confd.Value(title)),
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_format),   confd.Value((ns.hash, fmt), confd.C_IDENTITYREF)),
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_copies),   confd.Value(copies, confd.C_UINT32)),
    confd.TagValue(confd.XmlTag(ns.hash, ns.bz_shipping), confd.Value((ns.bz_shipping, ns.hash), confd.C_XMLEND))
  ]
  gm = time.gmtime(time.time())
  now = confd.DateTime(gm.tm_year, gm.tm_mon, gm.tm_mday, 
    gm.tm_hour, gm.tm_min, gm.tm_sec, 0, 0, 0)
  lowdp.notification_send(stream, now, values)

if __name__ == "__main__":
    try:
      d = Daemon(name='purchase-notification-sender-daemon', log=None)
      workersock = confd.dp.take_worker_socket(d, 'trader', 'send')
      trader = lowdp.register_notification_stream(d.ctx(), None, workersock, 'Trader')
      d.start()
      title = "What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action"
      send_shipping_notif(trader, user="janl", title=title, fmt=ns.bz_paperback, copies=1)
      confd.dp.return_worker_socket(d, 'send')
    finally:
      d.finish()
