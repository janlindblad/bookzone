######################################################################
# (C) 2019 Jan Lindblad
#
# See the LICENSE   file for license information
# See the README.md file for more information
######################################################################

usage:
	@echo "See README.md for more instructions"
	@echo "make all        # Build all example files"
	@echo "make clean      # Remove all built and intermediary files"
	@echo "make start      # Start ConfD daemon and example agent"
	@echo "make stop       # Stop any ConfD daemon and example agent"
	@echo "make reset      # Start over (make stop clean all start)"
	@echo "make nc         # See list of predefined NETCONF requests"
	@echo "make rc         # See list of predefined RESTCONF requests"
	@echo "make cli-c      # Start the ConfD Command Line Interface, C-style"
	@echo "make cli-j      # Start the ConfD Command Line Interface, J-style"

######################################################################
# Where is ConfD installed? Make sure CONFD_DIR points it out

# Include standard ConfD build definitions and rules
include $(CONFD_DIR)/src/confd/build/include.mk

/src/confd/build/include.mk:
	$(error CONFD_DIR not set. See README.md for details.)

######################################################################
.PHONY: clean all start stop rc nc

ifneq (,$(wildcard $(CONFD_DIR)/src/confd/pyapi))
# Your ConfD has Python support, let's use that by default
CONFD_WITH_PYTHON = true
ifndef USE_C
USE_PYTHON = true
endif
endif

CONFD_FLAGS = --addloadpath $(CONFD_DIR)/etc/confd 
PYTHON ?= python
START_FLAGS ?=

all:	bookzone-example.fxs apps audiozone-example.fxs \
		$(CDB_DIR) \
		$(CDB_DIR)/authors_init.xml \
		$(CDB_DIR)/books_init.xml \
		$(CDB_DIR)/users_init.xml \
		$(CDB_DIR)/payment_init.xml \
		ssh-keydir
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

ifdef USE_PYTHON
apps: bookzone_example_ns.py

bookzone_example_ns.py: bookzone-example.fxs
	$(CONFDC) --emit-python $@ $<

else
# No Python support in your ConfD, let's 
# build your purchase_action and send_notif
# handler from C
apps: purchase_action send_notif

OSTYPE  = $(shell uname -s)
CFLAGS  += -D$(OSTYPE)
SRC_pa  = purchase_action.c
OBJS_pa = $(SRC_pa:.c=.o)
SRC_sn  = send_notif.c
OBJS_sn = $(SRC_sn:.c=.o)

purchase_action: bookzone-example.h $(OBJS_pa)
	$(CC) -o $@ $(OBJS_pa) $(LIBS)

send_notif: bookzone-example.h $(OBJS_sn)
	$(CC) -o $@ $(OBJS_sn) $(LIBS)

endif

# ConfD will load any XML files in the database directory as 
# factory default data when it starts clean (no database files)
$(CDB_DIR)/%.xml: ../common/factory-defaults/%.xml
	cp $< $@

######################################################################
clean:  genclean pyclean cclean traceclean dbclean 

genclean: iclean
	-rm confderr.* 2> /dev/null || true

pyclean:
	-rm *_ns.py __init__.py *.pyc 2> /dev/null || true
	-rm -r __pycache__ 2> /dev/null || true

cclean:
	-rm bookzone-example.h ./purchase_action ./send_notif

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
	confd_load -l -m -O operational-data.xml

ifdef USE_PYTHON
start_apps:
	$(PYTHON) -m purchase_action $(START_FLAGS) &
else
start_apps:
	PATH=".:$$PATH" ./purchase_action $(START_FLAGS) &
endif

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


# You are triggering the event notification manually now. 
# In the real world, this would be sent automatically when
# some application detects the right conditions (i.e. books
# are in stock again)
ifdef USE_PYTHON
send-notif:
	$(PYTHON) -m send_notif
else
send-notif:
	./send_notif
endif

######################################################################
nc:
	@echo "Once ConfD is running, "
	@echo "you can use these make targets to make NETCONF requests:"
	@echo "make nc-hello            # YANG 1.0/1.1 capability and module discovery"	
	@echo "make nc-get-config       # Get-config of all configuration data"
	@echo "make nc-get-auths-books  # Get-config with XPATH and subtree filter"
	@echo "make nc-many-changes     # Edit-config with many changes (run once)"
	@echo "make nc-rollback-latest  # Rollback latest transaction"
	@echo "make nc-get-author       # Get the author of a single book"
	@echo "make nc-get-stock        # Get the stock qty of certain books"
	@echo "make nc-purchase-book    # Run action to buy a certain book"
	@echo "make nc-list-streams     # Get list of NETCONF notification streams"
	@echo "make nc-subscr-trader    # Subscribe to Trader NETCONF notifications"
	@echo "                           (hangs waiting for notifications to arrive)"
	@echo "make send-notif          # Sends Trader notification"

nc-hello: nc-hello-1.0 nc-hello-1.1

nc-hello-1.0:
	# List capabilities and YANG 1.0 modules
	netconf-console --hello

nc-hello-1.1:
	# Get YANG 1.1 capability and module-set-id
	netconf-console --hello | grep "urn:ietf:params:netconf:capability:yang-library:1.0"
	# List YANG 1.1 modules
	netconf-console --get --xpath /modules-state/module

nc-get-config:
	netconf-console --get-config

nc-get-auths-books: nc-get-auths-books-xpath nc-get-auths-books-subtree

nc-get-auths-books-xpath:
	# Get list of authors and books using XPATH filter
	netconf-console --get-config --xpath "/authors|books"

nc-get-auths-books-subtree:
	# Get list of authors and books using subtree filter
	netconf-console nc/get-authors-and-books-subtrees.nc.xml

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
	netconf-console --get --xpath '/books/book[count(format) &gt; 1][popularity &lt; 365]/format/number-of-copies/in-stock'

nc-purchase-book:
	netconf-console --rpc=nc/purchase-book.nc.xml

nc-list-streams:
	netconf-console --get --xpath /netconf-state/streams

nc-subscr-trader:
	# This session will now hang waiting for notifications to arrive.
	(echo kill $$$$ \# to terminate this subscription; exec netconf-console --create-subscription=Trader)

######################################################################
rc:
	@echo "Once ConfD is running, "
	@echo "you can use these make targets to make RESTCONF requests:"
	@echo "make rc-hello            # Find RESTCONF server and supported YANG modules"
	@echo "make rc-root-options     # List valid operations on root node"
	@echo "make rc-get-all-data     # Get all data from server"
	@echo "make rc-get-data1        # Get data at depth 1"
	@echo "make rc-get-books1       # Get books at depth 1"
	@echo "make rc-get-books2       # Get books at depth 2"
	@echo "make rc-get-books3       # Get books at depth 2, only certain fields"
	@echo "make rc-get-books4       # Get books at depth 2, only certain fields, as JSON"
	@echo "make rc-book-options     # List valid operations on book node"
	@echo "make rc-add-author       # Post a new author"
	@echo "make rc-update-price     # Put update a book price"
	@echo "make rc-update-prices    # Patch update several book prices"
	@echo "make rc-delete-book      # Delete a book"
	@echo "make rc-many-changes     # Yangpatch several changes in a single transaction"
	@echo "make rc-purchase-book    # Invoke action to purchase a book"
	@echo "make rc-list-streams     # Get list of RESTCONF notification streams"
	@echo "make rc-subscr-trader    # Subscribe to Trader RESTCONF notifications"
	@echo "                           (hangs waiting for notifications to arrive)"
	@echo "make send-notif          # Sends Trader notification"

rc-hello:
	# First query the well-known host-meta for the RESTCONF server
	curl -i -X GET http://localhost:8080/.well-known/host-meta --header "Accept: application/xrd+xml" -u admin:admin
	# Check for ietf-yang-library
	curl -i -X GET http://localhost:8080/restconf -u admin:admin
	# List ietf-yang-library
	curl -i -X GET http://localhost:8080/restconf/data/modules-state -u admin:admin

rc-root-options:
	curl -i -X OPTIONS http://localhost:8080/restconf -u admin:admin

rc-get-all-data:
	curl -i -X GET http://localhost:8080/restconf/data -u admin:admin

rc-get-data1:
	curl -i -X GET http://localhost:8080/restconf/data?depth=1 -u admin:admin

rc-get-books1:
	curl -i -X GET http://localhost:8080/restconf/data/books?depth=1 -u admin:admin

rc-get-books2:
	curl -i -X GET http://localhost:8080/restconf/data/books?depth=2 -u admin:admin

rc-get-books3:
	curl -i -X GET "http://localhost:8080/restconf/data/books?depth=2&content=config&fields=book/title;book/author " -u admin:admin

rc-get-books4:
	curl -i -X GET "http://localhost:8080/restconf/data/books?depth=2&fields=book/title;book/author" --header "Accept: application/yang-data+json" -u admin:admin

rc-book-options:
	curl -i -X OPTIONS "http://localhost:8080/restconf/data/books/book" -u admin:admin

rc-add-author:
	curl -i -X POST "http://localhost:8080/restconf/data/authors" --header "Content-Type: application/yang-data+json" --header "Accept: application/yang-data+json" --data @rc/add-kazuo-ishiguro.rc.json -u admin:admin

rc-update-price:
	curl -i -X PUT "http://localhost:8080/restconf/data/bookzone-example:books/book=The%20Hitchhiker%27s%20Guide%20to%20the%20Galaxy/format=9781400052929/price" --header "Accept: application/yang-data+json" --header "Content-Type: application/yang-data+json" --data '{ "price" : "38.0" }' -u admin:admin

rc-update-prices:
	curl -i -X PATCH "http://localhost:8080/restconf/data/bookzone-example:books/book" --header "Accept: application/yang-data+json" --header "Content-Type: application/yang-data+json" --data @rc/update-prices.rc.json -u admin:admin

rc-delete-book:
	curl -i -X DELETE "http://localhost:8080/restconf/data/bookzone-example:books/book=The%20Art%20of%20War" --header "Accept: application/yang-data+json" --header "Content-Type: application/yang-data+json" -u admin:admin

rc-many-changes:
	curl -i -X PATCH "http://localhost:8080/restconf/data" --header "Accept: application/yang-data+xml" --header "Content-Type: application/yang-patch+xml" --data @rc/many-changes.rc.yangpatch.xml -u admin:admin

rc-purchase-book:
	curl -i -X POST "http://localhost:8080/restconf/data/users/user=janl/purchase" --header "Content-Type: application/yang-data+json" --header "Accept: application/yang-data+json" --data @rc/purchase-book.rc.json -u admin:admin

rc-list-streams:
	curl -i -X GET http://localhost:8080/restconf/data/ietf-restconf-monitoring:restconf-state/streams -u admin:admin

rc-subscr-trader:
	# This session will now hang waiting for notifications to arrive.
	# Hit ^C to terminate the subscription
	curl -i -X GET http://localhost:8080/restconf/streams/Trader/json -u admin:admin

######################################################################
