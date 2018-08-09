######################################################################
# (C) 2018 Jan Lindblad
#
# See the LICENSE file for license information
# See the README  file for more information
######################################################################

usage:
	@echo "See README files for more instructions"
	@echo "make all        Build all example files"
	@echo "make clean      Remove all built and intermediary files"
	@echo "make start      Start ConfD daemon and example agent"
	@echo "make stop       Stop any ConfD daemon and example agent"
	@echo "make reset      make stop -> clean -> all -> start"
	@echo "make nc-reqs    See list of predefined NETCONF requests"
	@echo "make rc-reqs    See list of predefined RESTCONF requests"
	@echo "make cli-c      Start the ConfD Command Line Interface, C-style"
	@echo "make cli-j      Start the ConfD Command Line Interface, J-style"

######################################################################
# Where is ConfD installed? Make sure CONFD_DIR points it out

# Include standard ConfD build definitions and rules
include $(CONFD_DIR)/src/confd/build/include.mk

/src/confd/build/include.mk:
	$(error CONFD_DIR not set. See README for details.)

######################################################################

CONFD_FLAGS = --addloadpath $(CONFD_DIR)/etc/confd 
PYTHON ?= python
START_FLAGS ?=

all:	bookzone-example.fxs bookzone_example_ns.py audiozone-example.fxs \
		$(CDB_DIR) copy-factory-defaults ssh-keydir
	@echo "Build complete"

reset: stop clean all start

# Keeping make rules very simple and explicit so you can see what's going on

bookzone-example.fxs: bookzone-example.yang bookzone-example-ann.yang
	$(CONFDC) --fail-on-warnings -c \
						-o $@ \
						-a bookzone-example-ann.yang \
						bookzone-example.yang

audiozone-example.fxs: audiozone-example.yang audiozone-example-ann.yang
	$(CONFDC) --fail-on-warnings -c \
						-o $@ \
						-a audiozone-example-ann.yang \
						audiozone-example.yang

bookzone_example_ns.py: bookzone-example.fxs
	$(CONFDC) --emit-python $@ $<

copy-factory-defaults:
	# ConfD will load any XML files in the database directory as 
	# factory default data when it starts clean (no database files)
	cp factory-defaults/*_init.xml $(CDB_DIR)

######################################################################
clean:	genclean pyclean traceclean dbclean 

genclean: iclean

pyclean:
	-rm *_ns.py __init__.py *.pyc 2> /dev/null || true

traceclean:
	-rm *.trace 2> /dev/null || true

dbclean:
	-rm -f $(CDB_DIR)/*.cdb

######################################################################
start:  stop start_confd start_apps start_done

start_confd:
	$(CONFD) -c confd.conf $(CONFD_FLAGS)
	# Load some operational sample data
	# Must be done after ConfD has started
	confd_load -lCO operational-data.xml

start_apps:
	$(PYTHON) ./purchase_action.py $(START_FLAGS) &

start_done:
	# Give the purchase action a moment to start in the background
	@sleep 1
	@echo ""

######################################################################
stop:
	# Shutting down the confd daemon and indirectly any confd agents
	$(CONFD) --stop || true

######################################################################
cli-j:
	$(CONFD_DIR)/bin/confd_cli -J --user=admin --groups=admin \
		--interactive || echo Exit

cli-c:
	$(CONFD_DIR)/bin/confd_cli -C --user=admin --groups=admin \
		--interactive  || echo Exit

######################################################################
nc-reqs:
	@echo "Once ConfD is running, "
	@echo "you can use these make targets to make NETCONF requests:"
	@echo "make nc-hello            YANG 1.0/1.1 capability and module discovery"
	@echo "make nc-get-config       get-config with XPATH and subtree filter"
	@echo "make nc-many-changes     edit-config with many changes (run once)"
	@echo "make nc-rollback-latest  rollback latest transaction"
	@echo "make nc-get-author       get the author of a single book"
	@echo "make nc-get-stock        get the stock qty of certain books"
	@echo "make nc-purchase-book    run action to buy a certain book"
	@echo "make nc-list-streams     get list of NETCONF notification streams"
	@echo "make nc-subscr-trader    subscribe to Trader NETCONF notifications"
	@echo "                         (hangs waiting for notifications to arrive)"
	@echo "make nc-send-notif       sends Trader NETCONF notification"

nc-subscr-trader:
	netconf-console --create-subscription=Trader

nc-send-notif:


nc-hello: nc-hello-1.0 nc-hello-1.1

nc-hello-1.0:
	# List capabilities and YANG 1.0 modules
	netconf-console --hello

nc-hello-1.1:
	# Get YANG 1.1 capability and module-set-id
	netconf-console --hello | grep "urn:ietf:params:netconf:capability:yang-library:1.0"
	# List YANG 1.1 modules
	netconf-console --get --xpath /modules-state/module

nc-get-config: nc-get-config-xpath nc-get-config-subtree

nc-get-config-xpath:
	# Get list of authors and books using XPATH filter
	netconf-console --get-config --xpath "/authors|books"

nc-get-config-subtree:
	# Get list of authors and books using subtree filter
	netconf-console nc/get-config-subtree.nc.xml

nc-many-changes:
	# 1. Update author Michael Ende's account-id
	# 2. Delete author Sun Tzu (must exist)
	# 3. Add book The Buried Giant (may already exist)
	# 4. Update price of book The Neverending Story
	# 5. Remove book The Art of War (may be already absent)
	# 6. Add author Kazuo Ishiguro (must not already exist)
	netconf-console --rpc=nc/many-changes.nc.xml
	# Because of the 'must' restrictions above it will not
	# work to run this operation several times.
	# You may run   make nc-rollback-latest   to undo
	# this operation, then run again.

nc-rollback-latest:
	netconf-console --rpc=nc/rollback-latest.nc.xml

nc-get-author:
	netconf-console --get --xpath '/books/book[title="The Hitchhiker&apos;s Guide to the Galaxy"]/author'

nc-get-stock:
	netconf-console --get --xpath '/books/book[count(formats) &gt; 1][popularity &lt; 365]/formats/number-of-copies/in-stock'

nc-purchase-book:
	netconf-console --rpc=nc/purchase-book.nc.xml

nc-list-streams:
	netconf-console --get --xpath /netconf-state/streams

nc-subscr-trader:
	# This session will now hang waiting for notifications to arrive.
	(echo kill $$$$ \# to stop this waiting; exec netconf-console --create-subscription=Trader)

nc-send-notif:
	# You are triggering the event notification manually now. 
	# In the real world, this would be sent automatically when
	# some application detects the right conditions (i.e. books
	# are in stock again)
	$(PYTHON) ./send_notif.py

######################################################################
rc-reqs:
	@echo "Once ConfD is running, "
	@echo "you can use these make targets to make RESTCONF requests:"
	@echo "make rc-hello            YANG 1.0/1.1 capability and module discovery"


######################################################################
