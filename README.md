BookZone Example Project
========================

This project is an open source network programmability tutorial 
accompanying the book "Network Programmability with YANG: 
The Structure of Network Automation with YANG, NETCONF, RESTCONF, 
and gNMI". We hope you will find this project useful whether you
are reading the book or not. For the deeper insights, we would
certainly recommend reading the book, however.

The book can be ordered online from a variety of sources. The ISBN
is 978-0135180396 (or 0135180392 in the older ISBN format). Here are 
links to the [YANG book Amazon page] and the [BookZone project page].

[YANG book Amazon page]: https://www.amazon.com/Network-Programmability-YANG-Modeling-driven-Management/dp/0135180392
[BookZone project page]: https://github.com/janlindblad/bookzone


Software you will need
----------------------

The project is using a variety of tools. If you would like to run
everything, you will need to download and install the following
YANG, NETCONF, RESTCONF and gNXI software development kits (SDKs):

+ [ConfD Basic]
+ [gNXI]
+ [NSO]

[ConfD Basic]: https://www.tail-f.com/confd-basic/
[gNXI]: https://github.com/google/gnxi
[NSO]: https://developer.cisco.com/docs/nso/#!getting-nso/getting-nso

Once you have installed ConfD or NSO, you need to set a collection of
environment variables in order for the system to find the commands, 
sources and files necessary. The easiest way to do that is to source
the resource (rc) file that comes with each installation. This also
allows easily switching from one installed version to another or back
again within seconds.

If a bash user installed ConfD basic in \~/confd-basic/6.7/ the 
following command would set up the environment correctly:

> source \~/confd-basic/6.7/confdrc

Similarly, for an NSO user with NSO installed in \~/nso/4.7/ the
following command would set up the environment correctly:

> source \~/nso/4.7/ncsrc

Depending on your interests, you may not need all of these SDKs. The 
example descriptions in "The YANG Journey" below list the SDKs you 
will need for each one. Apart from these SDKs, you will also need the
following tools. Many of them are often already installed on a 
developer's machine, but you may want to make sure.

### make

Essential build tool, included in most development environments. How
to install depends on your system, but you could try one of these:

> sudo apt-get install build-essential
> yum install make

### curl

URL fetching tool. Here is the [curl] home page.
[curl]: https://github.com/curl/curl

You could also try one of these commands:
> sudo apt-get install curl
> yum install curl

### netconf-console

Basic NETCONF client. Here is the [netconf-console] home page.
[netconf-console]: https://pypi.org/project/netconf-console/

With some luck, you could also install it using
> pip install netconf-console

### Paramiko

Python SSH implementation. Here is the [Paramiko] installation page.
[Paramiko]: http://www.paramiko.org/installing.html

With some luck, you could also install it using
> pip install paramiko

### Pyang

Basic (extensible) YANG compiler. Here is the [Pyang] home page.
[Pyang]: https://pypi.org/project/pyang/

With some luck, you could also install it using
> pip install pyang


The YANG Joureney
-----------------

There are seven stages of YANG models in this project. If you are new
to YANG, you can start from the beginning and work your way through
to more complex modules. Or you can jump in at any particular step 
and start playing around and making your own changes and experiments.

### 1-intro/

This is the first, small and simple step towards a YANG module and 
running server. The module is tiny, just 30 lines, but complete 
enough to compile and allow starting a server and get to configure a 
few things on it. The system doesn't actually do anything based on 
what is configured, so you can feel safe experimenting. This section 
requires [ConfD Basic].

### 2-config/

The module from 1-intro is expanded with a couple of additional 
lists, and uses some more specific types, such as uses enumerations 
and identities and a typedef. This section requires [ConfD Basic].

### 3-action-notif/

This module adds actions and notifications to the system, and uses 
the leafref type. The project also provides some backend code for 
implementing the actions and notifications. This section requires
[ConfD Basic].

### 4-oper/

This version of the module adds operational data (read only status)
to the mix, and introduces a grouping. The module has now grown to 
just over 250 lines. This section requires [ConfD Basic].

### 5-precision/

In this version of the module, the existing leafs are refined with
more exact types, ranges, patterns and contstraints like properly 
linked leafrefs, must and when statements. At this point, the module 
is about 350 lines. This section requires [ConfD Basic].

### 6-augment/

Here the exact same bookzone-example.yang module is used as in
5-precision/, but an additional module audiozone-example.yang 
augments the former with additional elements. This allows a
different organization to evolve and tailor an externally sourced 
YANG module to their needs. This section requires [ConfD Basic].

### 7-service/

This section uses a completely different YANG module, working on the 
service level (not on the device level). The aim is to understand
service orchestration, and how that maps to device configuration.
This section requires [NSO].


The NETCONF, RESTCONF, and gNMI journey
---------------------------------------

These examples also allow testing out the details of NETCONF, 
RESTCONF and gNMI. In order to have a server to play with, these 
examples piggy-back on the YANG examples above.

### NETCONF

To play with NETCONF towards the ConfD server, go into 6-augment/ 
above and type

> make nc

To see a menu of specific operations you could run. This section
requires [ConfD Basic]. 

### RESTCONF

Similarly, for RESTCONF, type

> make rc

Unfortunately the RESTCONF functionality available in ConfD is not
currently included in the ConfD Basic package. In order to play with
RESTCONF, a ConfD Premium evaluation would be required.

### gNMI/gRPC

In order to play with gNMI, it is necessary to go into the 2-config/ 
directory. This is because the gNXI implementation currently does not
support YANG 1.1, which the higher YANG modules leverage. Once in 
that directory, type

> make gnmi

This will show a menu of commands you can run. This section requires 
the [gNXI] SDK.


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however.

Jan Lindblad
