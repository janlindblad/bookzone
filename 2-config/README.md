BookZone Example Project, 2-config stage
========================================

Purpose
-------

The module from 1-intro is expanded with a couple of additional 
lists, and uses some more specific types, such as uses enumerations 
and identities and a typedef. This section requires [ConfD Basic]
and corresponds to Example 3-12 in the book.
[ConfD Basic]: https://www.tail-f.com/confd-basic/

This stage contains the initial, tiny YANG module for the BookZone 
project. This stage gives you a first taste of YANG and the tools 
involved.


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
* confd.conf              <br> Configuration file for the ConfD 
                               server itself
* factory-defaults/       <br> Directory with initial (factory setup)
                               configuration data

Once you build the little system (see below), you will also find:

* bookzone-example.fxs    <br> A compiled, binary representation of 
                               the YANG file
* confd-cdb/              <br> ConfD's Database directory
* ssh-keydir/             <br> ConfD's directory for the SSH host 
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

Let's have a look at the initial configuration for this example. 
If you have followed the discussion in the book, you know that what 
we have been building is a management system for books. YANG is good
for managing pretty much anything. There is nothing in YANG that 
makes it particularly suited for network management, even if that is
what it has been used for mostly.

What we want to see now is the running configuration, but not all of 
it. Just the books, users and authors -- the things we have created  
models for in the YANG. Now we see our work in a CLI representation,
but it is also available in numerous programming APIs.

> 
`admin connected from 127.0.0.1 using console on JLINDBLA-M-W0J2`  
`JLINDBLA-M-W0J2#`  
`JLINDBLA-M-W0J2# ` **show running-config books**  
`books book "I Am Malala: The Girl Who Stood Up for Education and Was Shot by the Taliban"`  
` author "Malala Yousafzai"`  
` format 9780297870913`  
`  format-id hardcover`  
` !`  
`!`  
`books book "The Art of War"`  
` author   "Sun Tzu"`  
` language english`  
` format 160459893X`  
`  format-id paperback`  
`  price     12.75`  
` !`  
`!`  
`books book "The Hitchhiker's Guide to the Galaxy"`  
...

Next, let's show what users and authors we have.

> 
`JLINDBLA-M-W0J2# ` **show running-config users**  
`users user bc`  
` name "Benoît Claise"`  
`!`  
`users user janl`  
` name "Jan Lindblad"`  
`!`  
`users user joe`  
` name "Joe Clarke"`  
`!`  
`JLINDBLA-M-W0J2# ` **show running-config authors**  
`authors author "Douglas Adams"`  
` account-id 1010`  
`!`  
`authors author "Malala Yousafzai"`  
` account-id 1011`  
`!`  
`authors author "Michael Ende"`  
` account-id 1001`  
`!`  
`authors author "Per Espen Stoknes"`  
` account-id 1111`  
`!`  
`authors author "Sun Tzu"`  
` account-id 1100`  
`!`  

The way we made the BookZone YANG module, there is no top level 
container for all BookZone related content, so there is no single
command to show just these three things and nothing else. By 
tweaking the YANG a little, you could very well change that.
Maybe something for me to consider the next time I write a tutorial 
like this.

Now, let's add add a book to our catalog. While working in the CLI, 
remember to hit [TAB] to save some typing and see your options.

> 
`JLINDBLA-M-W0J2# ` **con**  
`Entering configuration mode terminal`  
`JLINDBLA-M-W0J2(config)# ` **books book [TAB]**  
`Possible completions:`  
`  <title:string>                                                                                                `  
`  I Am Malala: The Girl Who Stood Up for Education and Was Shot by the Taliban                                  `  
`  The Art of War                                                                                                `  
`  The Hitchhiker's Guide to the Galaxy                                                                          `  
`  The Neverending Story                                                                                         `  
`  What We Think About When We Try Not To Think About Global Warming: Toward a New Psychology of Climate Action  `  
`  range                                                                                                         `  

Ok, here are the book titles again, but that's not where I want to 
go. I want to add a new book, actually one of my favorites:

> 
`JLINDBLA-M-W0J2(config)# ` **books book "Actionable Gamification: Beyond Points, Badges, and Leaderboards"**  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **[TAB]**  
`Possible completions:`  
`  author     `  
`  format     `  
`  language   `  
`  ---        `  
`  commit     Commit current set of changes`  
`  describe   Display transparent command information`  
`  exit       Exit from current mode`  
`  help       Provide help information`  
`  no         Negate a command or set its defaults`  
`  pwd        Display current mode path`  
`  top        Exit to top level and optionally run command`  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **author "Yu-kai Chou" [TAB]**  
`                                                                                                      ^`  
`% Invalid input detected at '^' marker.`  

I typed in the name of the author, then hit [TAB]. That made the 
system check my input, and realize there is no author by that name
in list author, which makes this value invalid. The CLI complains 
that this won't work.

We fully intend to add this author before we commit the transaction, 
however, so let's go ahead with this anyway. The system is fine with 
this... for now.

> 
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# author "Yu-kai Chou"` **[ENTER]**  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# `  

Next up is the book language. Not marked mandatory in the YANG, so 
you could skip this step if you wanted. Hitting [TAB] reveals all the 
language options we modeled in the YANG.

> 
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **language [TAB]**  
`Possible completions:`  
`  arabic  chinese  english  french  moroccan-arabic  swahili  swedish`  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# language ` **english**  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# `  

Then we have format, which is keyed by ISBN number. We put some 
proper effort into describing the ISBN type quite exactly. The CLI 
tells us that there are two accepted formats for ISBN, one 10, and 
one 13 character variant. I'm lazy to look up the actual ISBN number 
so for now, I'll use some random 13 character string.

> 
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **format [TAB]**  
`Possible completions:`  
`  <isbn:string, must be exactly 10 chars>  <isbn:string, must be exactly 13 chars>  range`  
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **format willfindout23**  
`------------------------------------------------------------------------------------------------------^`  
`syntax error: "willfindout23" is not a valid value.`  

The system keeps me honest here. No cheating. Have to look up and 
use a real ISBN number. Well, I could probably have entered all 
zeroes or something too. The system isn't checking anything beyond 
the rules we put into the YANG. Additional checking would be allowed,
however, by the YANG standard. So if you feel like calculating
the check digit, or even going out to some external catalog system 
and verify that the title, author, format and ISBN all match up, by 
all means, be my guest.

> 
`JLINDBLA-M-W0J2(config-book-Actionable Gamification: Beyond Points, Badges, and Leaderboards)# ` **format 9781511744041**  
`Value for 'format-id' [audio-cd,epub,file-idty,hardcover,...]: ` **paperback**  

One more detail remains, the price. It's modeled to be optional, but 
the book will not be orderable until it has a price, so let's get 
that done right away.

> 
`JLINDBLA-M-W0J2(config-format-9781511744041)# ` **[TAB]**  
`Possible completions:`  
`  format-id   `  
`  price       `  
`  ---         `  
`  commit      Commit current set of changes`  
`  describe    Display transparent command information`  
`  exit        Exit from current mode`  
`  help        Provide help information`  
`  no          Negate a command or set its defaults`  
`  pwd         Display current mode path`  
`  top         Exit to top level and optionally run command`  
`JLINDBLA-M-W0J2(config-format-9781511744041)# ` **price 22.22**  
`JLINDBLA-M-W0J2(config-format-9781511744041)# `  

Finally done? Let's commit.

>  
`JLINDBLA-M-W0J2(config-format-9781511744041)# ` **commit**  
`Aborted: illegal reference 'books book "Actionable Gamification: Beyond Points, Badges, and Leaderboards" author'`  

Ah, we promised earlier that we'd make sure the author name we 
entered earlier would be added to the list of authors, remember?
The system sees a leafref, and respects it. So let's return to the 
top of the YANG, and go into container authors.

>  
`JLINDBLA-M-W0J2(config-format-9781511744041)# ` **top**  
`JLINDBLA-M-W0J2(config)# ` **authors author [TAB]**  
`Possible completions:`  
`  <name:string>  Douglas Adams  Malala Yousafzai  Michael Ende  Per Espen Stoknes  Sun Tzu  range`  
`JLINDBLA-M-W0J2(config)# authors author "Yu-kai Chou"`  
`JLINDBLA-M-W0J2(config-author-Yu-kai Chou)# `  

So let's see what we have before we have another go at committing.

>  
`JLINDBLA-M-W0J2(config-author-Yu-kai Chou)# ` **top**  
`JLINDBLA-M-W0J2(config)# ` **show c**  
`authors author "Yu-kai Chou"`  
`!`  
`books book "Actionable Gamification: Beyond Points, Badges, and Leaderboards"`  
` author   "Yu-kai Chou"`  
` language english`  
` format 9781511744041`  
`  format-id paperback`  
`  price     22.22`  
` !`  
`!`  
`JLINDBLA-M-W0J2(config)# ` **commit**  
`Commit complete.`  
`JLINDBLA-M-W0J2(config)# `  


Contributions
-------------

You are most welcome to contribute to this project with suggestions, 
bug reports and pull requests. Keep in mind that the examples have to 
stay very close to the contents of the book, however. Let's connect 
on the [BookZone project page].  
[BookZone project page]: https://github.com/janlindblad/bookzone

Jan Lindblad
