all:	ccl

# Include standard NCS examples build definitions and rules
include $(NCS_DIR)/src/ncs/build/include.ncs.mk

ccl:	../load-dir/ipython-superuser.ccl \
	../load-dir/ipython-superuser-j.ccl

../load-dir/%.ccl:	cli/%.cli
	$(NCSC) -c -o $@ $<

clean:
	rm -rf  ../load-dir/*.ccl
