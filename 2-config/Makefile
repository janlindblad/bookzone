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

CONFD_FLAGS = --addloadpath $(CONFD_DIR)/etc/confd 
PYTHON ?= python
START_FLAGS ?=

all:	bookzone-example.fxs \
		$(CDB_DIR) \
		$(CDB_DIR)/authors_init.xml \
		$(CDB_DIR)/books_init.xml \
		$(CDB_DIR)/users_init.xml \
		ssh-keydir
	@echo "Build complete"

reset: stop clean all start

# Keeping make rules very simple and explicit so you can see what's going on

bookzone-example.fxs: bookzone-example.yang
	$(CONFDC) --fail-on-warnings -c \
						-o $@ \
						bookzone-example.yang

# ConfD will load any XML files in the database directory as 
# factory default data when it starts clean (no database files)
$(CDB_DIR)/%.xml: ../common/factory-defaults/%.xml
	cp $< $@

######################################################################
clean:	genclean pyclean traceclean dbclean 

genclean: iclean
	-rm confderr.* 2> /dev/null || true

pyclean:
	-rm *_ns.py __init__.py *.pyc 2> /dev/null || true
	-rm -r __pycache__ 2> /dev/null || true

traceclean:
	-rm *.trace 2> /dev/null || true

dbclean:
	-rm -f $(CDB_DIR)/*.cdb

######################################################################
start:  stop start_confd

start_confd:
	$(CONFD) -c confd.conf $(CONFD_FLAGS)

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
