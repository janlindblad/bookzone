all: fxs
	cd ../netsim && $(MAKE) all
# Include standard NCS examples build definitions and rules
include $(NCS_DIR)/src/ncs/build/include.ncs.mk


JAVA_PACKAGE = com.example.juniperjunos
JDIR = $(shell echo $(JAVA_PACKAGE) | sed 's/\./\//g')

JFLAGS = --java-disable-prefix \
         --exclude-enums \
         --fail-on-warnings \
         --java-package $(JAVA_PACKAGE).$(NS) \
         --emit-java java/src/$(JDIR)/$(NS)

javac: fxs java-gen
	cd java && ant -q all

NS  = namespaces

SRC = $(wildcard yang/*.yang)
FXS = $(SRC:yang/%.yang=ncsc-out/modules/fxs/%.fxs)
DIRS = ncsc-out ../load-dir ../shared-jar ../private-jar        java/src/$(JDIR)/$(NS)

## Uncomment and patch the line below if you have a dependency to
## other YANG files
# YANGPATH += ../../<pkt-name>/src/yang

NCSCPATH   = $(YANGPATH:%=--yangpath %)

fxs: $(DIRS) ncsc-out/.done
.PHONY: fxs

$(DIRS):
	mkdir -p $@

ncsc-out/.done:	$(SRC)
	$(NCSC) --ncs-compile-bundle yang                        \
                 --ncs-device-dir ncsc-out   \
                 --ncs-device-type netconf  $(NCSCPATH)          \
                 $(NCSC_EXTRA_FLAGS) &&                          \
        cp ncsc-out/modules/fxs/*.fxs ../load-dir;

java-gen:
	for f in `echo ../load-dir/*.fxs`; do \
           n=`basename $$f | sed 's/\.fxs//'`; \
	   $(NCSC) $(JFLAGS)/$${n}.java $$f  || exit 1; \
        done
	touch -m ncsc-out/.done

clean:
	rm -rf $(DIRS)
	cd ../netsim && $(MAKE) clean || true
	cd java && ant -q clean || true
	rm -f java/src/$(JDIR)/$(NS)/*.java
