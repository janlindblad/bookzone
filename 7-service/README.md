BookZone Example Project, 7-service stage
=========================================

Purpose
-------

This example develops a service model that is loaded into the Network
Services Orchestrator [NSO]. NSO exposes this model to its clients, 
so that they can manage the function on the network layer, in a more
abstract way than interacting directly with devices. Instead, NSO 
takes care of the device interacton, leveraging the network-wide 
transactions available in NETCONF.

NSO in turn will load and use collections of device YANG modules for
a number of different device types.

This section requires [NSO] and corresponds to chapter 10 in 
the book. 
[NSO]: https://developer.cisco.com/site/nso/


Checking your tools
-------------------

Check the README.md file in the project root folder to see which 
tools you will want to ensure you have installed.


What's in this Directory
------------------------

In this directory you will initially find just a few files:
  
* README.md               <br> This file
* LICENSE                 <br> States the BSD license terms for this 
                               tutorial
* Makefile                <br> Assembly instructions for the example
* ncs-cdb/                <br> NSO's database directory
* packages/               <br> NSO's package directory. This is where
                               all the NSO applications live
* ncs.conf                <br> NSO's configuration file
* publishers-init.xml     <br> Sample configuration data for a few
                               publishers
* stores-init.xml         <br> Sample configuration data for a few
                               stores

Under packages/ you will find a collection of NSO components:

* cisco-iosxe/            <br> NED for Cisco IOS-XE devices
* id-allocator/           <br> ID-allocation component that plugs in
                               with the resource manager component
* ipython-superuser/      <br> Python development utility component
* juniper-junos/          <br> NED for Juniper JunOS devices
* netrounds-ncc/          <br> NED for Netrounds NCC devices
* resource-manager/       <br> Resource management framework 
                               compoennt
* storeconnect/           <br> Store connect service package 
                               developed for this example

NED packages are Network Element Drivers, i.e. packages that contain 
the device YANG modules for that type of device. For devices that do 
not support NETCONF, the NED package would also contain code 
necessary to interface with that device type.

Each package contains both sources and build artifacts. The sources
are found under:

* package-meta-data.xml   <br> Manifest file that declares to NSO 
                               what the contents of the package is
* src/                    <br> YANG and Java source files
* templates/              <br> Service template files
* python/                 <br> Python source files

Once a package is built, the build results are found in:

* load-dir/               <br> The compiled YANG files (.fxs)
* private-jar/            <br> Package implementation Java files
* shared-jar/             <br> Package interface Java files

Python, being a just-in-time compiled language, do not need compiled
files, and if compiled files are generated, they are next to the
source files.


Build & Start
-------------

To see what make targets there are, simply run make with no arguments

> `$` **make**

This will show a menu of available make targets. To build and start 
the system, you would type

> `$` **make all start**

Later, when you'd like to shut it down, you could type

> `$` **make stop clean**

If you want to shut down, but keep all your data intact, don't 
include clean. Provided you've built first, you can start and stop 
the server whenever you like by just issuing

> `$` **make start**  
> `$` **make stop**  

Building the NED packages may take a while as they contain hundreds
of YANG modules. While building the NEDs, you may also see a rather 
large amount of warnings. This is expected. Some of them are because 
the device YANG modules have issues. Some are due to NSO not being 
able to calculate all dependencies on its own. The user could add
dependency information in annotation files, but for our use here that
will not be necessary.


Suggested Steps
---------------

%% To be written %%

Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
