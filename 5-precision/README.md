BookZone Example Project, 5-precision stage
===========================================

Purpose
-------

The module from 4-oper is enhanced by describing the information with
greater precision, so that all parties can better understand what 
data makes sense, and what the format should be.

This section requires [ConfD Basic] and corresponds to chapter 3 in 
the book, up to and including Example 3-34. 
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

Constraints are important to keep a YANG model meaningful. The 
constraints prevent operations that would otherwise leave the system 
in an inconsistent state, or remove data that makes little sense.

For example, the inventory in our book catalogue only makes sense for 
physical items, and not for items that are delivered by copying a 
file. In the bookzone-example.yang, we placed a YANG when-expression
on the inventory data which makes sure file based inventory items are
not present in the data tree.

If we list show the book catalog contents for The Neverending Story, 
we can see there are two items. One is a paperback edition, the other
an mp3 recording of it. The mp3 edition is different than a CD-book
in that it is delivered by file copy, not on a physical medium.

`# ` **show running-config books book The\ Neverending\ Story|tab**  
`TITLE                  AUTHOR        LANGUAGE  ISBN           FORMAT ID  PRICE  `  
`--------------------------------------------------------------------------------`  
`The Neverending Story  Michael Ende  english   9780140386332  paperback  8.5    `  
`                                               9781452656304  mp3        29.95  `  
` `
`!`

The |tab (pipe tab) at the end of the command ensures the data is
displayed in tabular format. If we display the inventory data for 
this title, we can see the CLI adds hyphens - for the elements that
don't exist according to the YANG data model.

`# ` **show books book The\ Neverending\ Story | tab**               
`                                                  IN                          `  
`TITLE                  POPULARITY  ISBN           STOCK  RESERVED  AVAILABLE  `  
`------------------------------------------------------------------------------`  
`The Neverending Story  47          9780140386332  4      0         4          `  
`                                   9781452656304  -      -         -          `  

Or, perhaps even more clearly in non-tabular form.

`# ` **show books book The\ Neverending\ Story | notab**  
`books book "The Neverending Story"`  
` popularity 47`  
` format 9780140386332`  
`  number-of-copies in-stock 4`  
`  number-of-copies reserved 0`  
`  number-of-copies available 4`  
` format 9781452656304`  

There simply are no number-of-copies data for the mp3 edition of the
title (isbn 9781452656304).

Similarly, we added some constraints on the book catalogue itself.
For example, every book in the catalogue must have an author. We can 
demonstrate this constraint in action if we delete one of the authors
in the sample data.

`# ` **con**  
`Entering configuration mode terminal`  
`(config)# ` **no authors author**  
`Possible completions:`  
`  Douglas Adams  Malala Yousafzai  Michael Ende  Per Espen Stoknes  Sun Tzu  <cr>`  
`(config)# ` **no authors author Michael\ Ende**  
`(config)# ` **commit**  
`Aborted: illegal reference 'books book "The Neverending Story" author'`  
`(config)# no books book The\ Neverending\ Story `  
`(config)# commit`  
`Commit complete.`  

At first, when we try to delete the author and commit, the system
refuses, as that would violate the constraint that all books must 
have an author. If we delete the book(s) that reference this author,
the whole transaction works fine. The order that this happens within
the transaction is irrelevant. Transactions do not have time 
internally, there is no before and after inside a transaction.

Since we are (well I am at least) sorry to see this book go, let's
get it back again. It's easy using a rollback command. The rollback
simply loads a file with undo information that the system generates 
for each transaction. Once the undo information is loaded into the
transaction, it can be viewed and modified at will. It's just another
transaction like any other. No magic there.

`(config)# ` **rollback configuration**  
`(config)# ` **show c**  
`authors author "Michael Ende"`  
` account-id 1001`  
`!`  
`books book "The Neverending Story"`  
` author   "Michael Ende"`  
` language english`  
` format 9780140386332`  
`  format-id paperback`  
`  price     8.5`  
` !`  
` format 9781452656304`  
`  format-id mp3`  
`  price     29.95`  
` !`  
`!`  
`(config)# commit`  
`Commit complete.`  

Adding back the book works fine as long as we are also adding back
the author no later than in the same transaction. It doesn't matter
in which order these objects are (re-)created within the transaction.
Transactions do not have internal time.

Since every transaction generates a rollback file, we can of course
undo the operation that added back the author and book title. And 
revert that again with yet another rollback. It is also possible to
rollback selective transactions from the transaction history, or only
parts of a transaction, and to edit the result before committing it.

`(config)# ` **rollback configuration**  
`(config)# ` **commit**
`Commit complete.`
`JLINDBLA-M-W0J2(config)# ` **rollback configuration**
`JLINDBLA-M-W0J2(config)# ` **show c**                
`authors author "Michael Ende"`  
` account-id 1001`  
`!`  
`books book "The Neverending Story"`  
` author   "Michael Ende"`  
` language english`  
` format 9780140386332`  
`  format-id paperback`  
`  price     8.5`  
` !`  
` format 9781452656304`  
`  format-id mp3`  
`  price     29.95`  
` !`  
`!`  
`# ` **show running-config books book The\ Neverending\ Story|tab**  
`TITLE                  AUTHOR        LANGUAGE  ISBN           FORMAT ID  PRICE  `  
`--------------------------------------------------------------------------------`  
`The Neverending Story  Michael Ende  english   9780140386332  paperback  8.5    `  
`                                               9781452656304  mp3        29.95  `  

The YANG constraints pertain equally to all management interfaces, 
such as NETCONF and RESTCONF as well.


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
