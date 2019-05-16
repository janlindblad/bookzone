import os
import time
time.sleep(1)

import ncs
ncs_trans_id=int(os.environ['NCS_MAAPI_THANDLE'])
ncs_sess_id=int(os.environ['NCS_MAAPI_USID'])
maapi=ncs.maapi.Maapi()
trans=maapi.attach(ncs_trans_id)
root=ncs.maagic.get_root(trans)
print "+-----------------------------------------------------------------------------+"
print "| You may reference the current transaction maagic YANG root object as 'root' |"
print "| E.g.   In [1]: for dev in root.devices.device:                              |"
print "|          ...:     print dev.name                                            |"
print "+-----------------------------------------------------------------------------+"
