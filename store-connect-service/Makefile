all: packages build-sim-network
start: start-netsim start-ncs
stop: stop-netsim stop-ncs
clean: clean-packages clean-sim-network clean-logs
dist-clean: clean clean-db clean-state
.PHONY: packages

packages:
	for i in packages/*/src; do make -C $$i all; done

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
