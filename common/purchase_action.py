"""
*********************************************************************
* YANG Book Purchase Action                                         *
* Derived from: ConfD Actions intro example                         *
*                                                                   *
* (C) 2018 Jan Lindblad                                             *
*                                                                   *
* See the LICENSE file for license information                      *
* See the README  file for more information                         *
*********************************************************************
"""
from __future__ import print_function

import sys
import time

import confd
from confd.dp import Action, Daemon
from confd.maapi import Maapi
from confd.log import Log


#logger class used by Daemon
class MyLog(object):
    def info(self, arg):
        print("info: %s" % arg)
    def error(self, arg):
        print("error: %s" % arg)

class PurchaseAction(Action):
    @Action.action
    def cb_action(self, uinfo, name, kp, input, output):
        self.log.info("purchase invoked")
        output.out_of_stock.create()
        self.log.info("unfortunately we're temporarily out of stock")

def load_schemas():
    with Maapi():
        pass

if __name__ == "__main__":
    load_schemas()
    logger = Log(MyLog(), add_timestamp=True)
    d = Daemon(name='purchase-action-handler-daemon', log=logger)

    a = []
    a.append(PurchaseAction(daemon=d, actionpoint='purchase', log=logger))

    try:
        d.start()
        logger.info('--- Purchase action handler daemon STARTED ---')
        while d.is_alive(): 
            time.sleep(10)
    except: #e.g. KeyboardInterrupt:
        pass
    finally:
        try:
            d.finish()
            logger.info('--- Purchase action handler daemon FINISHED ---')
        except:
            pass
