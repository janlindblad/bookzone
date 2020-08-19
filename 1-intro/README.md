BookZone Example Project, 1-intro stage
=======================================

Purpose
-------

This stage contains the initial, tiny YANG module for the BookZone 
project. This stage gives you a first taste of YANG and the tools 
involved.

This section requires [ConfD Basic] and corresponds to Example 3-2 in
the book.
[ConfD Basic]: https://www.tail-f.com/confd-basic/


Checking your tools
-------------------

If you have everything set up correctly, you should be able to 
execute all of the following commands without receiving an error
message.

> `$` **make --version**  
> `$` **confdc --version**  


What's in this Directory
------------------------

In this directory you will initially find just a few files:
  
* README.md               <br> This file
* LICENSE                 <br> States the BSD license terms for this 
                               tutorial
* Makefile                <br> Assembly instructions for the example
* bookzone-example.yang   <br> The YANG file that describes the 
                               interface to the system we're building
* confd.conf              <br> Configuration file for the ConfD 
                               server itself

Once you build the little system (see below), you will also find:

* bookzone-example.fxs    <br> A compiled, binary representation of 
                               the YANG file
* confd-cdb               <br> ConfD's Database directory
* ssh-keydir              <br> ConfD's directory for the SSH host 
                               keys


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

### Configuring via Command Line Interface (CLI)

In order to use the ConfD server, it's worthwhile to get familiar 
with the CLI it offers. There are two variants of the CLI you can
choose from, the C-style and J-style. Pick the one that suits you
best. For the tutorial here, we will use the C-style CLI. The syntax
is a little different in the J-style, but all the commands are 
available in the J-style CLI as well.

You can start the CLI by typing

> 
`$` **make cli-c**  
`/Users/jlindbla/confd/basic-6.6/bin/confd_cli -C --user=admin --groups=admin \`  
`      --interactive  || echo Exit`  
`Welcome to ConfD Basic`  
` `  
`The CLI may only be used during product development.`  
`Commercial use of the CLI is prohibited.`  
`You must disable the CLI in confd.conf for production.`  
`For more information, visit http://www.tail-f.com.`  
`admin connected from 127.0.0.1 using console on JLINDBLA-M-W0J2`  
`JLINDBLA-M-W0J2# `

You are now in the operational mode CLI. You could display the 
current running configuraion. You will find only the (boring) factory 
default configuration in there:

> 
`JLINDBLA-M-W0J2# ` **show running-config**  
`aaa authentication users user admin`  
` uid        9000`  
` gid        20`  
` password   $1$4ekMncmX$3htoQX0I3o80b3.wGCIp71`  
` ssh_keydir /var/confd/homes/admin/.ssh`  
` homedir    /var/confd/homes/admin`  
`!`  
`aaa authentication users user oper`  
...  
`JLINDBLA-M-W0J2# `  

To add a book, first we need to go into configuration mode. Hitting 
the [TAB]-key displays the completions at that point.

> 
`JLINDBLA-M-W0J2# ` **config**  
`Entering configuration mode terminal`  
`JLINDBLA-M-W0J2(config)# ` **[TAB]**  
`Possible completions:`  
`  aaa        AAA management`  
`  alias      Create command alias.`  
`  books      `  
`  nacm       Access control`  
`  session    Global default CLI session parameters`  
`  user       User specific command aliases and default CLI session parameters`  
`  ---        `  
`  abort      Abort configuration session`  
`  annotate   Add a comment to a statement`  
...  
`JLINDBLA-M-W0J2(config)# `  

The part we want to configure is inside books, so type a 'b' and hit
the [TAB] key to let the system fill in 'books' for you. Hit [TAB] 
again to get the next word 'book'. If you hit [TAB] yet again to at
this point, nothing comes up. Because there is nothing configured 
under this location yet. If you hit question mark ?, you will see the
type of the data expected at this point:

> 
`JLINDBLA-M-W0J2(config)# ` **books book ?**  
`Possible completions:`  
`  <title:string>  range`  
`JLINDBLA-M-W0J2(config)# `  

Type double quotes and the name of the book you want to add:

> 
`JLINDBLA-M-W0J2(config)# ` books book **"The Neverending Story"**  

When you hit [ENTER] the system detects that the ISBN leaf is 
mandatory, so by default prompts you for a value:

> 
`Value for 'isbn' (<string>): `  

Fill in a the isbn value for the book:

> 
`Value for 'isbn' (<string>): ` **9780140386332**  

This will now leave you in the book The Neverending Story "submode". 
Hitting [TAB] here shows you the options:

> 
`JLINDBLA-M-W0J2(config-book-The Neverending Story)# ` **[TAB]**  
`Possible completions:`  
`  author     `  
`  isbn       `  
`  price      `  
`  ---        `  
`  commit     Commit current set of changes`  
`  describe   Display transparent command information`  
`  exit       Exit from current mode`  
`  help       Provide help information`  
`  no         Negate a command or set its defaults`  
`  pwd        Display current mode path`  
`  top        Exit to top level and optionally run command`  
`JLINDBLA-M-W0J2(config-book-The Neverending Story)# `  

You can add a few more facts about the book here, then return to the 
top level mode.

> `JLINDBLA-M-W0J2(config-book-The Neverending Story)# ` **author "Michael Ende"**  
> `JLINDBLA-M-W0J2(config-book-The Neverending Story)# ` **price 8.50**  
> `JLINDBLA-M-W0J2(config-book-The Neverending Story)# ` **top**  
> `JLINDBLA-M-W0J2(config)# `  

Once back at the top level, showing the current configuration changes 
is easy using the "show c" command, short for show configuration.

> 
`JLINDBLA-M-W0J2(config)# ` **show c**  
`books book "The Neverending Story"`  
` isbn   9780140386332`  
` author "Michael Ende"`  
` price  8.5`  
`!`  
`JLINDBLA-M-W0J2(config)# `  

To store this configuration persistently in the database, you need to
commit it:

> 
`JLINDBLA-M-W0J2(config)# ` **commit**  
`Commit complete.`  
`JLINDBLA-M-W0J2(config)# `  

Very good. You have successfully added and persisted the addtion.
Let's check that the book is there. To show the full configuration in
config mode, use the command "show full-configuration", or "show f"
for short. Hit 'q' to exit the show command when it stops at the 
first full page.

> 
`JLINDBLA-M-W0J2(config)# ` **show full-configuration**  
`books book "The Neverending Story"`  
` isbn   9780140386332`  
` author "Michael Ende"`  
` price  8.5`  
`!`  
`aaa authentication users user admin`  
` uid        9000`  
` gid        20`  
` password   $1$4ekMncmX$3htoQX0I3o80b3.wGCIp71`  
` ssh_keydir /var/confd/homes/admin/.ssh`  
` homedir    /var/confd/homes/admin`  
`!`  
`aaa authentication users user oper`  
...  
`Aborted: by user`  
`JLINDBLA-M-W0J2(config)# `  

Now that we have a little bit of config in the system, let's leave 
the ConfD CLI and retrieve this over NETCONF. Type exit to get out of 
the configuration mode into operational mode, then again to get 
completely out of the ConfD CLI. Hitting [Ctrl+D] also works.

> 
`JLINDBLA-M-W0J2(config)# ` **exit**  
`JLINDBLA-M-W0J2# ` **exit**  
`JLINDBLA-M-W0J2:1-intro jlindbla$ `  

### Retrieving the configuration using netconf-console

The netconf-console tool is a simple NETCONF client, making it easy
to send requests to NETCONF servers, especially ConfD. There are many 
defaults in netconf-console that match ConfD default settings, making 
it easy to use with very little typing.

To get the full configuration we just created in the ConfD CLI, for 
example, a single flag is needed:

> 
`JLINDBLA-M-W0J2:1-intro jlindbla$ ` **netconf-console --get-config**  
`<?xml version="1.0" encoding="UTF-8"?>`  
`<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="1">`  
`  <data>`  
`    <books xmlns="http://example.com/ns/bookzone">`  
`      <book>`  
`        <title>The Neverending Story</title>`  
`        <isbn>9780140386332</isbn>`  
`        <author>Michael Ende</author>`  
`        <price>8.5</price>`  
`      </book>`  
`    </books>`  
`    <aaa xmlns="http://tail-f.com/ns/aaa/1.1">`  
`      <authentication>`  
`        <users>`  
`          <user>`  
`            <name>admin</name>`  
...  
`  </data>`  
`</rpc-reply>`  

This response is rather long, however, so it may make sense to only 
ask for the /books section of the configuration:

>
`JLINDBLA-M-W0J2:1-intro jlindbla$ ` 
**netconf-console --get-config --xpath /books**  
`<?xml version="1.0" encoding="UTF-8"?>`  
`<rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="1">`  
`  <data>`  
`    <books xmlns="http://example.com/ns/bookzone">`  
`      <book>`  
`        <title>The Neverending Story</title>`  
`        <isbn>9780140386332</isbn>`  
`        <author>Michael Ende</author>`  
`        <price>8.5</price>`  
`      </book>`  
`    </books>`  
`  </data>`  
`</rpc-reply>`  
`JLINDBLA-M-W0J2:1-intro jlindbla$ `  


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
