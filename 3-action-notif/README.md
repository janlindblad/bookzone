BookZone Example Project, 3-action-notif stage
==============================================

Purpose
-------

The module from 2-config is expanded with a "purchase" action and 
"shipping" notification. You can invoke the purchase action to 
purchase a book. When the book is ready to ship, the system would
issue the shipping notification. In this example, you will have to
tell the system when to generate the notification.

This section requires [ConfD Basic] and corresponds to Example 3-15 
in the book. The action and notification handlers are implementated
twice; once in Python and once in C. Apart from being instructive,
this is necessary since ConfD Basic doesn't provide Python APIs.
[ConfD Basic]: https://www.tail-f.com/confd-basic/

If you have obtained ConfD Premium, which has Python API support,
the Makefile will automatically switch to use that. Whichever ConfD
edition you have, feel free to compare the sources.


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
* bookzone-example.yang   <br> The YANG file that describes the 
                               interface to the system we're building
* bookzone-example-ann.yang<br> Annotation file with implementation
                               details for the bookzone-example.yang
* confd.conf              <br> Configuration file for the ConfD 
                               server itself
* purchase_action.py      <br> Purchase action handler in Python
* purchase_action.c       <br> Purchase action handler in C.
                               Only one of these two will be used
* send_notif.py           <br> Shipping notification sender in Python
* send_notif.c            <br> Shipping notification sender in C.
                               Only one of these two will be used

Once you build the little system (see below), you will also find:

* bookzone-example.fxs    <br> A compiled, binary representation of 
                               the YANG file
* confd-cdb/              <br> ConfD's Database directory
* ssh-keydir/             <br> ConfD's directory for the SSH host 
                               keys
* bookzone_example_ns.py  <br> Python file with constants
                               representing all the YANG symbols in 
                               the bookzone_example module.
                               Needed by send_notif.py, but not by
                               purchase_action.py
* bookzone-example.h      <br> C header file with constants
                               representing all the YANG symbols in 
                               the bookzone_example module.
* __init__.py             <br> File telling the Python VM that there
                               are Python source files in this
                               directory.
* purchase_action         <br> Executable when building C handler
                               application
* send_notif              <br> Executable when building C sender
                               application
* \*.o                    <br> Object files


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
