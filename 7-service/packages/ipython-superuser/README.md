# IPython-superuser

Your IPython NSO hood-opener with superuser powers

# Purpose

IPython-superuser grants any NSO CLI Operator superuser access to an
ipython environment which can see and modify anything in the
Operator's transaction.

# SECURITY WARNING

By installing this package, any operator can become 'superuser' and
read and write any part of the configuration, including decryption of
encrypted data such as passwords, read any non-configuration data,
invoke any action. The operator may also gain full access to the
underlaying operating system with the same privileges as NSO.

You can use traditional NSO NACM access control rules to lock down who
can execute the 'ipython' command. Unless specifically configured, all
operators will have access by default.

**LET ME STRESS AGAIN:** __Anyone who can execute this command will
leave the realm of NACM and have full control over all data and
actions NSO can reach, including encrypted data. NACM rules will
not apply as long as the operator stays in the IPython
environment. From within IPython, the operator will also have the
same acess to the underlaying operating system as NSO itself. An
operator could easily start up a traditional shell and run any
OS-level commands that NSO has privilege to execute. If NSO runs
as root, that means access to all commands and files in the
underlaying operating system.__

# Dependencies

In order to run all of the functionality, you will need to have (in
the path) the following components. If something is missing, your most
important use cases may still work, but some tools will not.

* NSO 4.2+
* Python 2.7+ or 3+ with ipython support

# Build instructions

Normal NSO package build:

    make -C packages/ipython-superuser/src/ clean all

# Usage examples

## Listing the device names

Just type ipython on an operational or configuration prompt to get started.

    JLINDBLA-M-J8L9# ipython
    Python 2.7.11 (default, Jan 22 2016, 08:29:18)  
    Type "copyright", "credits" or "license" for more information.

    IPython 5.1.0 -- An enhanced Interactive Python.
    ?         -> Introduction and overview of IPython's features.
    %quickref -> Quick reference.
    help      -> Python's own help system.
    object?   -> Details about 'object', use 'object??' for extra details.
    +-----------------------------------------------------------------------------+
    | You may reference the current transaction maagic YANG root object as 'root' |
    | E.g.   In [1]: for dev in root.devices.device:                              |
    |          ...:     print dev.name                                            |
    +-----------------------------------------------------------------------------+

Hit tab as you type past root. to complete the name, or to get a menu
of available matching choices.

    In [1]: for dev in root.devices.device:
    ...:     print dev.name
        ...:     
    ce0
    ce1
    ce2
    ce3

## Returning to NSO CLI

From operational mode, no assignments will be possible. When you're
done, exit to get back to NSO

    In [2]: exit
    JLINDBLA-M-J8L9# 

## Running a query

A few python variables are available in your session. The most
important one is usually `root`, which is the MAAGIC root object. It
represents the root of the YANG model. As seen above, you can use it to
navigate anywhere in the YANG.

The current transaction object is available as `trans`. You can use
this object to run a query:

    In [32]: trans.query_start(expr="/devices/device[port='8300']",
        context_node='/', chunk_size=10, initial_offset=0, result_as=1,
        select=['name'], sort=['name'])
    Out[32]: 3957

    In [33]: for res in maapi.query_result(Out[32]):
        ...:     print res
        ...:     
    ['/ncs:devices/device{p3}/name']
    ['/ncs:devices/device{pe2}/name']
    ['/ncs:devices/device{xr-local}/name']

## Decrypting passwords

Or decrypt a password. Here we also need to use the `maapi` and `ncs`
objects to install a copy of the NSO crypto keys in the IPython
environment and call the `decrypt` method.

    In [1]: root.devices.authgroups.group.keys()
    Out[1]: 
    [Key values = [<_ncs.Value type=C_BUF(5) value='default'>],
     Key values = [<_ncs.Value type=C_BUF(5) value='vagrant'>],
     Key values = [<_ncs.Value type=C_BUF(5) value='zenoss'>]]

    In [2]: root.devices.authgroups.group['default'].umap
    Out[2]: List name=umap tag=2113235867

    In [3]: root.devices.authgroups.group['default'].umap.keys()
    Out[3]: 
    [Key values = [<_ncs.Value type=C_BUF(5) value='admin'>],
     Key values = [<_ncs.Value type=C_BUF(5) value='oper'>]]

    In [4]: root.devices.authgroups.group['default'].umap['admin'].remote_password
    Out[4]: '$8$o8y+hdI1DWjvnlPH0XHaQKAvWxofjrba1rgb62IKZ/E='

    In [5]: maapi.install_crypto_keys()

    In [6]: ncs._ncs.decrypt(Out[4])
    Out[6]: 'admin'

## Syncing from all cisco-ios devices

Running sync-from with all devices that run the cisco-ios CLI NED.

    In [22]: for dev in root.devices.device:
        ...:     if dev.device_type.cli.exists() and dev.device_type.cli.ned_id == 'cisco-ios':
        ...:         print "Syncing from %s"%dev.name
        ...:         dev.sync_from()
        ...:         

## Changing host name on IOS device

Let's check and configure the ce0 hostname.

    In [1]: root.devices.device['ce0'].config.ios__hostname

Ok, none set.

    In [2]: root.devices.device['ce0'].config.ios__hostname = 'ce0'

    In [3]: root.devices.device['ce0'].config.ios__hostname
    Out[3]: 'ce0'

Now set. Return to NSO CLI to commit.

    In [4]: exit
    JLINDBLA-M-J8L9(config)# show c
    devices device ce0
     config
      ios:hostname  ce0
     !
    !
    JLINDBLA-M-J8L9(config)# commit

## Configuring BGP logging on all IOS-XR NETCONF devices

    In [3]: for dev in root.devices.device:
       ...:     if 'http://cisco.com/ns/yang/Cisco-IOS-XR-ipv4-bgp-cfg' in [key[0] for key in dev.capability.keys()]:
       ...:         print "Enabling BGP neighbor best path logging on %s"%dev.name
       ...:         dev.config.ipv4_bgp_cfg__bgp.instance.create('default').instance_as.create('1').four_byte_as.create('1').default_vrf.ipv4_bgp_cfg__global.ne
       ...: ighbor_logging_detail.create()
       ...:         
    Enabling BGP neighbor best path logging on p3
    Enabling BGP neighbor best path logging on pe2
    Enabling BGP neighbor best path logging on xr-local
    
    In [4]: exit
    JLINDBLA-M-J8L9(config)# show c
    devices device p3
     config
      ipv4-bgp-cfg:bgp instance default
       instance-as 1
        four-byte-as 1
         default-vrf global neighbor-logging-detail
        !
       !
      !
     !
    !
    devices device pe2
     config
      ipv4-bgp-cfg:bgp instance default
       instance-as 1
        four-byte-as 1
         default-vrf global neighbor-logging-detail
        !
       !
      !
     !
    !
    devices device xr-local
     config
      ipv4-bgp-cfg:bgp instance default
       instance-as 1
        four-byte-as 1
         default-vrf global neighbor-logging-detail
        !
       !
      !
     !
    !
    JLINDBLA-M-J8L9(config)# commit

# Troubleshooting

## ipython-superuser.py not found

You see this message:

    [TerminalIPythonApp] WARNING | File not found: u'packages/ipython-superuser/python/ipython-superuser.py'

This means ipython-superuser can't find it's initialization
file. Typically this would happen if NSO wasn't started from the
runtime directory (where the packages/ directory resides). In this
case you may need to update the ipython-superuser.cli file with the
correce path, rebuild the package, reload packages and try again.

## not writable

You see this message:

    Error: item is not writable (4): 

This means you're trying to modify the configuration, but started the
ipython session from operational mode. Exit out of IPython, enter
config mode and try again.  But don't despair, the up-arrow command
history is preserved.

# Contact

Contact Jan Lindblad <jlindbla@cisco.com> with any suggestions or
comments.
