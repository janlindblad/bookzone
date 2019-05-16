all: packages/id-allocator packages/loop packages/resource-manager

build: all

packages/loop: packages
	rm -rf packages/loop
	(cd packages; ln -s ../../package/loop .)

packages/id-allocator: packages
	rm -rf packages/id-allocator
	(cd packages; ln -s ../../../../id-allocator .)

packages/resource-manager: packages
	rm -rf packages/resource-manager
	(cd packages; ln -s ../../../../resource-manager .)

packages:
	ncs-netsim create-network cisco-ios 1 c
	ncs-setup --netsim-dir ./netsim --dest .

start:
	ncs-netsim start
	ncs --with-package-reload
	@echo "ncs_started"

stop:
	ncs --stop
	ncs-netsim stop

clean:
	rm -rf README.ncs logs ncs-cdb ncs.conf packages scripts state netsim

cli:
	ncs_cli -u admin

test:
	lux test.lux

clean_cdb:
	-rm -f ncs-cdb/*.cdb 2> /dev/null || true
	-rm -f ncs-cdb/rollback* 2> /dev/null || true

