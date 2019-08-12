BookZone Example Project, 4-oper stage
======================================

Purpose
-------

The module from 3-action-notif is expanded with operational status
information, such as how many copies of different items are in stock,
what their popularity is and the purchase history of users.

This section requires [ConfD Basic] and corresponds to chapter 3 in 
the book, up to and including Example 3-23. 
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
* operational-data.xml    <br> Factory default operational data 
                               loaded into the database at system 
                               start
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

This stage is about working with operational data. This is data that
is normally computed by the managed system, and provided to the user
so that you can understand the operational state of the system. In 
this example, all the operational data is static, fake data fed into 
the database, and remains there until you delete or modify it.

From a YANG perspective, the operational data is often located under
configuration data, because the operational data says something about
a configured object. In this example, the operational data consists 
of inventory data for each title in our book catalogue, and a
popularity metric (number of copies sold of this title in the last
12 months).

To see the entire book catalogue and all the associated operational
data, simply show books

> `# ` **show books**  
`TITLE                                                    POPULARITY  ISBN           STOCK  RESERVED  AVAILABLE`  
`--------------------------------------------------------------------------------------------------------------`  
`I Am Malala: The Girl Who Stood Up for Education ...     89          9780297870913  12     2         10`         
`The Art of War                                           -           160459893X     -      -         - `         
`The Hitchhiker's Guide to the Galaxy                     289         0330258648     32     3         29`         
`                                                                     9781400052929  3      -         3 `         
`The Neverending Story                                    47          9780140386332  4      0         4 `         
`                                                                     9781452656304  -      -         - `         
`What We Think About When We Try Not To Think About ...   17          9781603585835  2      2         0 `         

Entries marked with a dash - has no value at all, not even zero.

The book catalogue could be very long. It's also possible to display 
the data for a particular title (a table row). Just specify the 
title. Use tab-completion to save yourself from excessive typing. 
Note how the command line interface escapes each space in the title 
value with a backslash. This is normal.

`# ` **show books book [TAB]**  
`Possible completions:`  
`  I Am Malala: The Girl Who Stood Up for Education and Was Shot by the Taliban                                `  
`  The Art of War                                                                                              `  
`  The Hitchhiker's Guide to the Galaxy                                                                        `  
`  The Neverending Story                                                                                       `  
`  What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action`  
`  |                                                                                                           `  
`  <cr>                                                                                                        `  
`Possible match completions:`  
`  format  popularity`  
`JLINDBLA-M-W0J2# ` **show books book The H[TAB][TAB]**  
`Possible completions:`  
`  0330258648  9781400052929  |  <cr>`  
`Possible match completions:`  
`  number-of-copies`  
`# ` show books book The\ Hitchhiker's\ Guide\ to\ the\ Galaxy format 0[TAB]
`# ` show books book The\ Hitchhiker's\ Guide\ to\ the\ Galaxy format 0330258648 
`            IN                          `  
`ISBN        STOCK  RESERVED  AVAILABLE  `  
`----------------------------------------`  
`0330258648  32     3         29         `  

Similarly, a particular column can be displayed like this.

`# ` **show books book format number-of-copies available**  
`TITLE                                                    ISBN           AVAILABLE`  
`---------------------------------------------------------------------------------`  
`I Am Malala: The Girl Who Stood Up for Education ...     9780297870913  10       `  
`The Art of War                                           160459893X     -        `  
`The Hitchhiker's Guide to the Galaxy                     0330258648     29       `  
`                                                         9781400052929  3        `  
`The Neverending Story                                    9780140386332  4        `  
`                                                         9781452656304  -        `  
`What We Think About When We Try Not To Think About ...   9781603585835  0        `  

By adding a value at the end, we could for example display all books 
that are out of stock at the moment.

`# ` **show books book format number-of-copies available 0**
`TITLE                                                    POPULARITY  ISBN           STOCK  RESERVED  AVAILABLE`  
`--------------------------------------------------------------------------------------------------------------`  
`What We Think About When We Try Not To Think About ...   17          9781603585835  2      2         0        `  
` `  
`# `  

Similar queries can be formulated using NETCONF and RESTCONF using 
subtree filtering. If the server also supports XPath filters, even 
more advanced queries can formulated than shown above. Both subtree
and XPath filters are demonstrated in section 6-augment.


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
