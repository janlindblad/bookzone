######################################################################
# (C) 2018 Jan Lindblad
#
# See the LICENSE file for license information
# See the README  file for more information
######################################################################

usage:
	@echo "See README files for more instructions"
	@echo "make all     Build all example files"
	@echo "make clean   Remove all built and intermediary files"
	@echo "make start   Start CONFD daemon and example agent"
	@echo "make stop    Stop any CONFD daemon and example agent"
	@echo "make queries See list of predefined queries"
	@echo "make cli-c   Start the CONFD Command Line Interface, C-style"
	@echo "make cli-j   Start the CONFD Command Line Interface, J-style"

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

all:	bookzone-example.fxs bookzone-example_ns.py audiozone-example.fxs \
		$(CDB_DIR) factory-config ssh-keydir
	@echo "Build complete"

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

bookzone-example_ns.py: bookzone-example.fxs
	$(CONFDC) --emit-python $@ $<

factory-config:
	# ConfD loads any xml files in the database directory as factory
	# default config on the first start
	cp *init.xml $(CDB_DIR)

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
	@sleep 1
	@echo ""

######################################################################
stop:
	### Killing any confd daemon or confd agents
	$(CONFD) --stop    || true

######################################################################
cli-j:
	$(CONFD_DIR)/bin/confd_cli -J --user=admin --groups=admin \
		--interactive || echo Exit

cli-c:
	$(CONFD_DIR)/bin/confd_cli -C --user=admin --groups=admin \
		--interactive  || echo Exit

######################################################################
queries:
	@echo "NETCONF:"
	@echo "nc-hello       YANG 1.0/1.1 capability and module discovery"
	@echo "nc-get-config  get-config with XPATH and subtree filter"
	@echo "RESTCONF:"


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
	netconf-console nc/get-config-subtree.nc

#nc-edit-config:
#	netconf-console nc/get-config-subtree.nc

######################################################################
