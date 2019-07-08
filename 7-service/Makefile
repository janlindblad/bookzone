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
	@echo "make dist-clean # Also remove database and NSO internal state"
	@echo "make start      # Start NETSIM devices and NSO"
	@echo "make stop       # Stop NETSIM devices and NSO"

######################################################################
# Where is NSO installed? Make sure NCS_DIR points it out

# Check that NCS_DIR points to a proper NSO installation
include $(NCS_DIR)/netsim/confd/src/confd/build/vsn.mk

/netsim/confd/src/confd/build/vsn.mk:
	$(error NCS_DIR not set. See README.md for details.)

######################################################################

all: packages build-sim-network
start: start-netsim start-ncs
stop: stop-netsim stop-ncs
clean: clean-packages clean-sim-network clean-logs
dist-clean: clean clean-db clean-state
.PHONY: packages

packages:

	for i in \
			netrounds-ncc \
			juniper-junos \
			cisco-iosxe \
			ipython-superuser \
			resource-manager \
			id-allocator \
			storeconnect \
		; do make -C packages/$$i/src all; done

clean-packages:
	for i in packages/*/src; do make -C $$i clean; done

start-ncs:
	ncs --with-package-reload-force

stop-ncs:
	ncs --stop

build-sim-network:
	ncs-netsim create-network packages/juniper-junos 2 ej
	ncs-netsim add-to-network packages/cisco-iosxe 1 ec
	ncs-netsim add-to-network packages/cisco-iosxe 2 ic
	ncs-netsim add-to-network packages/juniper-junos 1 ij
	ncs-netsim add-to-network packages/netrounds-ncc 1 m

clean-sim-network:
	ncs-netsim delete-network

start-netsim:
	ncs-netsim start
	
stop-netsim:
	ncs-netsim stop

clean-logs:
	-rm logs/*

clean-db:
	-rm ncs-cdb/*.cdb

clean-state:
	-rm -rf state/*
	-rm -rf target/*
