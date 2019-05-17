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
	@echo "make cli-c      # Start the ConfD Command Line Interface, C-style"
	@echo "make cli-j      # Start the ConfD Command Line Interface, J-style"

######################################################################
# Where is ConfD installed? Make sure CONFD_DIR points it out

# Include standard ConfD build definitions and rules
include $(CONFD_DIR)/src/confd/build/include.mk

/src/confd/build/include.mk:
	$(error CONFD_DIR not set. See README.md for details.)

######################################################################

ifneq (,$(wildcard $(CONFD_DIR)/src/confd/pyapi))
	# Your ConfD has Python support, so let's use that
  CONFD_WITH_PYTHON = true
endif

CONFD_FLAGS = --addloadpath $(CONFD_DIR)/etc/confd 
PYTHON ?= python
START_FLAGS ?=

all:	bookzone-example.fxs apps \
		$(CDB_DIR) \
		$(CDB_DIR)/authors_init.xml \
		$(CDB_DIR)/books_init.xml \
		$(CDB_DIR)/users_init.xml \
		ssh-keydir
	@echo "Build complete"

reset: stop clean all start

# Keeping make rules very simple and explicit so you can see what's going on

bookzone-example.fxs: bookzone-example.yang bookzone-example-ann.yang
	$(CONFDC) --fail-on-warnings -c \
						-o $@ \
						-a bookzone-example-ann.yang \
						bookzone-example.yang


ifdef CONFD_WITH_PYTHON
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
clean:	genclean pyclean cclean traceclean dbclean 

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

ifdef CONFD_WITH_PYTHON
start_apps:
	PYTHONPATH=".:$$PYTHONPATH" $(PYTHON) ./purchase_action.py $(START_FLAGS) &
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
ifdef CONFD_WITH_PYTHON
send-notif:
	PYTHONPATH=".:$$PYTHONPATH" $(PYTHON) ./send_notif.py
else
send-notif:
	./send_notif
endif

######################################################################
