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

Looking for action? Let's jump right in. To purchase a book, invoke
the purchase action from the ConfD CLI like this

> 
`# ` **users user janl purchase title "What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action" format paperback number-of-copies 1**  
`out-of-stock`  

The purchase action is defined under list user in the YANG. It was 
modeled this way since users are the active part in this operation
that concerns books and users. Feels like the proper "object 
oriented" way of modeling this, but it could have been done in 
several other ways.

If you are interested to see how this action is invoked over NETCONF
or RESTCONF, have a look in section 6-augment/.

Executing this action will immediately (and not at commit time)
invoke the action implementation code. This code typically ensure my
purchase was delivered to me in a timely fashion, and generate a
action response back to the operator.

The implementation code that actually does something interesting
(see purchase_action.py) consists of a single line:

> `output.out_of_stock.create()`  

This means the action will invariably simply return an out-of-stock 
response. Talk about a lazy implementor! Anyway, injecting some 
imagination, it feels reasonable to assume that a system could have
done the needful. Even so, an out of stock situation could arise,
in which case the delivery would be delayed. In that situation, the
system would send out a notification when the delivery was about to
happen.

Now we have to play the role of the system and decice when it is time
to send that notification. When you feel the right time has come, run

> 
`$ ` **./send_notif**  
`Trader Shipping notification delivered`  

Again, the implementation of the notification sender is a bit on the
simplistic side. The interesting code (see send_notif.py) consists of
two lines:

> 
`title = "What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action"`  
`send_shipping_notif(trader, user="janl", title=title, fmt=ns.bz_paperback, copies=1)`  

It always sends a notification for this precise title, format and a 
single copy, no matter what you may have purchased previously.

Q: So who received this notification now? 
A: Nobody!

Nobody had subscribed for this kind of notifications. ConfD knew 
this, so never actually sent any notifications. 

It's easy to add a subscriber using the netconf-console NETCONF 
client. Before we can subscribe, we need to know the name of the
subscription stream. NETCONF servers may have any number of
NETCONF streams for different purposes. In ConfD they are listed in
the confd.conf file:

>
`  <notifications>`  
`    <eventStreams>`  
`      <stream>`  
`        <name>Trader</name>`  
`        <description>BookZone trading and delivery events</description>`  
`        <replaySupport>true</replaySupport>`  
`        <builtinReplayStore>`  
`          <enabled>true</enabled>`  
`          <dir>./confd-cdb</dir>`  
`          <maxSize>S10M</maxSize>`  
`          <maxFiles>50</maxFiles>`  
`        </builtinReplayStore>`  
`      </stream>`  
`    </eventStreams>`  
`  </notifications>`  

This means the stream name is 'Trader' (this is case sensitive). 
Let's create the subscription using netconf-console

> `$ ` **netconf-console --create-subscription Trader**  

This will actually send

>
`<?xml version="1.0" encoding="UTF-8"?>`  
`<rpc xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="1">`  
`  <create-subscription xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">`  
`    <stream>Trader</stream>`  
`  </create-subscription>`  
`</rpc>`  

The NETCONF server responds immediately with an ok message, but then
keeps the session open. When something happens, a notification will 
be delivered here. This delivery mechanism is called a long-running
RPC. It basically never ends.

>
`<?xml version="1.0" encoding="UTF-8"?>`  
`<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="1">`  
`  <ok/>`  
`</rpc-reply>`  

Hint: if you want to get out of this, try hitting ctrl+z, ^Z. If your
shell is a bash shell, you can kill the backgrounded process with 
kill %1

At this point, nothing happens with the subscription until someone
runs the send_notif program. Either do that in a separate terminal
window, or hit ^Z, run send_notif, and return to your subscription
with the fg command. 

>
`$ ` **./send_notif**  
`Trader Shipping notification delivered`  

The notification will then be displayed immediately.

>
`<?xml version="1.0" encoding="UTF-8"?>`  
`<notification xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">`  
`  <eventTime>2019-08-06T13:26:04.559832+00:00</eventTime>`  
`  <shipping xmlns="http://example.com/ns/bookzone">`  
`    <user>janl</user>`  
`    <title>What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action</title>`  
`    <format xmlns:bz="http://example.com/ns/bookzone">bz:paperback</format>`  
`    <number-of-copies>1</number-of-copies>`  
`  </shipping>`  
`</notification>`  


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
