BookZone Example Project, 6-augment stage
=========================================

Purpose
-------

The functionality in our bookzone module is extended by adding a
separate module audiozone that augments the former, demonstrating how
YANG modules can evolve over time and be extended without changing
the original module.

This section requires [ConfD Basic] and corresponds to the complete
and finished module at the end of chapter 3.
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

In this section we'd like to demonstrate that the augmented leafs
appear embedded in the original model, just like any other.

For instance, the augmenting model adds a "recommendations" leaf under
books book. It works just like any other leaf, as you would expect.

`# ` **config**  
`Entering configuration mode terminal`  
`(config)# ` **books book [TAB]**  
`Possible completions:`  
`  <title:string>`  
`  I Am Malala: The Girl Who Stood Up for Education and Was Shot by the Taliban`  
`  The Art of War`  
`  The Hitchhiker's Guide to the Galaxy`  
`  The Neverending Story`  
`  What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action`  
`(config)# books book ` **What[TAB]**  
`Possible completions:`  
`  author  format  language  recommendations  <cr>`  
`(config)# books book What\ We\ Think\ About\ When\ We\ Try\ Not\ To\ Think\ About\ Global\ Warming:\ Toward\ a\ New\ Psychology\ of\ Climate\ Action ` **recommendations ?**  
`Possible completions:`  
`  <review-date:dateTime (CCYY-MM-DDTHH:MM:SS)>`  
`(config)# books book What\ We\ Think\ About\ When\ We\ Try\ Not\ To\ Think\ About\ Global\ Warming:\ Toward\ a\ New\ Psychology\ of\ Climate\ Action recommendations` **2023-02-06T09:00:00 [TAB]**  
`Possible completions:`  
`  review-comment  score  <cr>`  
`(config)# books book What\ We\ Think\ About\ When\ We\ Try\ Not\ To\ Think\ About\ Global\ Warming:\ Toward\ a\ New\ Psychology\ of\ Climate\ Action recommendations 2023-02-06T09:00:00 ` **review-comment Daunting score 4**  
`(config-recommendations-2023-02-06T09:00:00-00:00)#` **exit**  
`(config-book-What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action)# ` **recommendations 2023-02-09T10:45:00 review-comment "Scaring prospects" score 3**  
`(config-recommendations-2023-02-09T10:45:00-00:00)#` **top**  
`(config)# ` **show c**  
`books book "What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action"`  
` recommendations 2023-02-06T09:00:00-00:00`  
`  score          4`  
`  review-comment Daunting`  
` !`  
` recommendations 2023-02-09T10:45:00-00:00`  
`  score          3`  
`  review-comment "Scaring prospects"`  
` !`  
`!`  

Similarly, the audiozone augmentations adds a concept "friends" to 
the audiozone users. 

`(config)# ` **users user [TAB]**  
`Possible completions:`  
`  <user-id:string>  bc  janl  joe`  
`(config)# users user ` **j [TAB]**  
`Possible completions:`  
`  janl  joe`  
`(config)# users user j` **anl [TAB]**  
`Possible completions:`  
`  friends  name  payment-methods  purchase  <cr>`  
`(config)# users user janl `  
`(config-user-janl)# ` **friends [TAB]**  
`Possible completions:`  
`  "Benoît Claise"  "Jan Lindblad"  "Joe Clarke"  [`  
`(config-user-janl)# friends ` **[ "Benoît Claise" "Joe Clarke" ]**  
`(config-user-janl)# ` **sh c**  
`users user janl`  
` friends [ "Benoît Claise" "Joe Clarke" ]`  
`!`  
`(config-user-janl)# ` **comm**  
`Commit complete.`  
`(config-user-janl)# ` **top**  
`(config)# ` **show full-configuration users**  
`users user bc`  
` name "Benoît Claise"`  
`!`  
`users user janl`  
` name    "Jan Lindblad"`  
` friends [ "Benoît Claise" "Joe Clarke" ]`  
` payment-methods payment-method paypal 4711.1234.0000.1234`  
` !`  
` payment-methods payment-method klarna 1234.4711.1234.0001`  
` !`  
`!`  
`users user joe`  
` name "Joe Clarke"`  
`!`  
`(config)# `

Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
