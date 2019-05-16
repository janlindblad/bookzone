# Introduction

The problem we are trying to solve is that of a generic resource
allocation mechanism that works well with services and in a high
availability (HA) configuration.  We want to be able to connect to
external resource allocators as well as using allocators that are
implemented in Java in NCS.  We want to have different allocators for
different resources.

Since the create in a service code isn't allowed to perform side
effects, ie do callouts, we use the reactive fastmap design pattern.

Furthermore, we want a design where we can swap allocators depending
on the deployment scenario, ie a plug and play design.

We solve this be creating one interface package called
resource-manager, and expect specific resource allocation
implementations to be implemented as separate NCS packages.

There must to be a package for handling each resource that will be
allocated, but one package can be swapped for another as long as they
perform the job of allocating resources that are requested through the
interface in the resource-manager package.

## Reactive Fastmap for Resource Allocation

The basic idea is as follows. A service does not allocate a resource
directly, instead it creates a configuration item stating that it
requests a resource. This request will be written to the DB when the
transaction commits. A CDB-subscriber detects that a resource request
has been added, allocates the resource using some mechanism of its
choice, writes the result to a CDB-oper leaf, and re-deploys the
service.

When re-deployed the service will perform the same allocation request
as the first time, and check the CDB-oper leaf to see if the result is
present. This time the result will be there and the service create
code can proceed.

When the service is deleted the resource allocation request written by
the service will be removed from CDB and the CDB-subscriber will be
notified that the allocation has been removed. It can then release the
resource.

## HA Considerations

It is important that the resource manager will work well in a HA
setup.  This concerns both the resource manager itself, and all
allocator packages.  They must all be ready for failover at any given
time, and the must not try to perform the allocation on more that one
node at any given time.

An allocated must make sure its state is replicated on all failover
nodes, or stored externally. An allocator is free to use CDB-oper for
this purpose.

# Design

We have created a package called resource-manager that has a data
model for two different resource pools: ids and ip addresses. Each
pool has an `allocation` list where services are expected to create
instances to signal that they request an allocation. Request
parameters are stored in the `request` container and the allocation
response is written in the `response` container.

Since the allocation request may fail the response container contains
a choice where one case is for error and one for success.

Each allocation list entry also contains an `allocating-service`
leaf. This is an instance-identifier that points to the service that
requested the resource. This is the service that will be re-deployed
when the resource has been allocated.

Resource allocation packages should subscribe to several points in
this resource-pool tree.  First, they must detect when a new resource
pool is created or deleted, secondly they must detect when an
allocation request is created or deleted. A package may also augment
the pool definition with additional parameters, for example, an ip
address allocator may wish to add configuration parameters for
defining the available subnets to allocate from, in which case it must
also subscribe to changes to these settings.

## Interface

The resource manager package contains a shared Java library with
functions for creating allocation requests, and for reading the
responses. These can be used from the service create code. For
example, the functions for creating a ip subnet allocation request
looks like this:

```
    public static void subnetRequest(NavuNode service, NavuNode root,
                                     String poolName,
                                     int cidrmask, String id)
        throws NavuException, ConfException, ResourceException;

    public static ConfIPPrefix subnetRead(Cdb cdb, String poolName, String id)
        throws ResourceException, NavuException, ConfException;
```

The `subnetRequest` function will create the subnet allocation request
as configuration in the resource manager data tree. If the pool does
not exist it will throw an ResourceException.

The `subnetRead` function will read the response leaves and return the
allocation result.  If no result exists it will throw a
`ResourceWaitException`, and if an error was returned from the
allocator it will throw a `ResourceErrorException`.

A service may use these function like this (code taken from the
example service found at `ipaddress-allocator/test/package/loop`

```
    public Properties create(ServiceContext context,
                             NavuNode service,
                             NavuNode root,
                             Properties opaque) throws DpCallbackException {

        NavuContainer cdbroot = null;

        try {
            // Get the container at keypath
            // /services/loop{s1}

            NavuContainer loop = (NavuContainer) service;

            ConfBuf devName = (ConfBuf) loop.leaf("device").value();
            String poolName = loop.leaf("pool").value().toString();
            String serviceName = loop.leaf("name").value().toString();

            // Create resource allocation request
            ResourceAllocator.subnetRequest(service, root, poolName, 32,
                                            serviceName);

            try {
                // Check if resource has been allocated
                ConfIPPrefix net =
                    ResourceAllocator.subnetRead(cdb, poolName, serviceName);

                ConfBuf unit = (ConfBuf) loop.leaf("unit").value();

                root.container("ncs","devices").
                    list("device").
                    elem(new ConfKey(devName)).
                    container("config").
                    container("ios","interface").list("Loopback").
                    sharedCreate(new ConfKey(unit));
            } catch (ResourceWaitException e) {
                // done, wait for re-deploy
                ;
            } catch (ResourceErrorException e) {
                // error
                System.err.println("Failed to allocate: "+e);
            }

        } catch (Exception e) {
            throw new DpCallbackException("Could not instantiate service", e);
        } finally {
            try {
                if (cdbroot != null)
                    cdbroot.stopCdbSession();
            }
            catch (Exception ignore) {
            }
        }
        return opaque;
    }
```

The way this is intended is that a service may request multiple
resources and then read the result. If any of the resource are not yet
present it will end up in the catch for `ResourceWaitException` and
return, waiting to be re-deployed. The specific resource allocator
will re-deploy the service when it has allocated the resource. For
example, if the specific resource allocation implementation performs
an RPC to allocate the resource from some external entity it will
eventually get a response, when it does it write the response into the
`response` container and re-deploys the service.

Similar APIs are available for id and vlan allocation.

A service can, of course, create the configuration items directly, and
inspect the oper result leaves instead of using the above Java
functions.

## Resource Allocator Data Model

The resource manager data mode is defined as:

```
  grouping resource-pool-grouping {
    leaf name {
      tailf:info "Unique name for the pool";
      type string;
    }

    leaf-list tags {
      tailf:info "Annotations assigned to this resource pool";
      type string;
    }

    list allocation {
      key id;

      leaf id {
        type string;
      }

      leaf allocating-service {
        tailf:info "Instance identifier of service that owns resouce";
        type instance-identifier;
      }

      container request {
      }

      container response {
        config false;
        tailf:cdb-oper {
          tailf:persistent true;
        }
        choice response-choice {
          case error {
            leaf error {
              type string;
            }
          }
          case ok {
          }
        }
      }
    }
  }

  container resource-pools {

    list vlan-id-pool {
      key name;

      tailf:info "VLAN id pool";
      uses resource-pool-grouping {
        augment "allocation/response/response-choice/ok" {
          leaf id {
            type uint16;
          }
        }
      }
    }

    list id-pool {
      tailf:info "Id pool";
      key name;

      uses resource-pool-grouping {
        augment "allocation/response/response-choice/ok" {
          leaf id {
            type uint16;
          }
        }
      }
    }

    list ip-address-pool {
      tailf:info "IP Address pools";
      key name;

      uses resource-pool-grouping {
        augment "allocation/request" {
          leaf subnet-size {
            mandatory true;
            type uint8;
          }
        }
        augment "allocation/response/response-choice/ok" {
          leaf subnet {
            type inet:ip-prefix;
          }
        }
      }
    }
```

# IP Address Allocator

The package `ipaddress-allocator` contains an ip address allocator
that use the resource-manager API to provide ip address allocation. It
uses a RAM based allocation algorithm that stores its state in CDB as
oper data.

The file
`ipaddress-allocator/src/java/src/com/cisco/nso/ipaddressallocator/IPAddressAllocator.java`
contains the part that deals with the resource manager APIs whereas
the RAM based IP address allocator resides under
`ipaddress-allocator/src/java/src/com/cisco/nso/ipam`

The IPAddressAllocator class subscribes to three points in the DB

* `/ralloc:resource-pools/ip-address-pool`

  To be notified when new pools are created/deleted. It needs to
  create/delete instances of the IPAddressPool class. Each instance of
  the IPAddressPool handles one pool.j

* `/ralloc:resource-pools/ip-address-pool/subnet`

  To be notified when subnets are added/removed from an existing
  address pool.  When a new subnet is added it needs to invoke the
  `addToAvailable` method of the right IPAddressPool instance. When a
  pool is removed it needs to reset all existing allocations from the
  pool, create new allocations, and re-deploy the services that had
  the allocations.

* `/ralloc:resource-pols/ip-address-pool-allocation`

  To detect when new allocation requests are added, and when old
  allocations are released. When a new request is added the right size
  of subnet is allocated from the IPAddressPool instance, and the
  result is written to the `response/subnet` leaf, and finally the
  service is re-deployed.

## Making the IPAddressPool Store its State in CDB

To make the state of the IPAddressPool implementation survive restarts
of NCS it needs to store its state persistently. The base
implementation use two `java.util.Set` implementations (HashSet and
TreeSet) to store available subnets, and allocations in RAM.

We created two classes that extended HashSet and TreeSet respectively.
These classes read their state from CDB when they are created, and any
changes are stored in CDB as well as in RAM. The data model below is
used for the CDB oper data (ipaddress-allocator.yang)

```
  container ip-allocator {
    config false;

    tailf:cdb-oper {
      tailf:persistent true;
    }

    list pool {
      key name;

      leaf name {
        type string;
      }

      list subnet {
        key "address cidrmask";
        tailf:cli-suppress-mode;

        leaf address {
          type inet:ip-address;
        }

        leaf cidrmask {
          type uint16;
        }

        description
          "Copy of configured subnets. Needed to know when subnets
           have been added.";
      }

      list available {
        key "address cidrmask";
        tailf:cli-suppress-mode;

        leaf address {
          type inet:ip-address;
        }

        leaf cidrmask {
          type uint16;
        }

        description
          "Free subnets available for allocation.";
      }

      list allocation {
        key "address cidrmask";
        tailf:cli-suppress-mode;

        leaf address {
          type inet:ip-address;
        }

        leaf cidrmask {
          type uint16;
        }

        leaf owner {
          type string;
        }

        description
          "Allocated subnets.";
      }
    }
  }
```

This has the side effect that it is easy to inspect the state of the
allocator from the NCS CLI. The operational command `show status
ip-allocator` in the J-style CLI will show the allocated and available
subnets at any given time.  In the C-style CLI the command `show
ip-allocator | notab` can be used (in NCS 3.3 and later also `show
ip-allocator | display curly-braces`.

## HA Considerations

There are two things we need to consider - the allocator state needs
to be replicated, and the allocation needs to only be performed on one
none.

The easiest way to replicate the state is to write it into CDB-oper and let CDB
perform the replication. This is what we do in the ipaddress-allocator.

We only want the allocator to allocate addresses on the master
node. Since the allocations are written into CDB they will be visible
on both master and slave nodes, and the CDB subscriber will be
notified on both nodes. In this case we only want the allocator on the
master node to perform the allocation. We therefore read the HA mode
leaf from CDB to determine which HA mode the current subscribe is
running in, if none or master, we proceed with the allocation.

# Test

In order to test the allocator there is a simple service and a lux
script that verifies that it works. The lux script is located at
`ipaddress-allocator/test/ncs/test.lux`

