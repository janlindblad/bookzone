package com.cisco.nso.idallocator;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.cisco.nso.idallocator.namespaces.idAllocator;
import com.cisco.nso.idpool.Allocation;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfUInt32;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuContext;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;

class AllocationsSet extends HashSet<Allocation> {
    private static Logger LOGGER = Logger.getLogger(AllocationsSet.class);

    private String poolName;

    public NavuList cdbAllocations;

    public AllocationsSet(NavuContext operContext, String poolName) {
        super();

        System.err.println("createing AllocationsSet");

        this.poolName = poolName;

        // pupulate from allocaitons stored in CDB

        try {
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer root = base.container(idAllocator.hash);
            NavuContainer ipallocator =
                root.container(idAllocator.prefix,
                               idAllocator._id_allocator_);
            NavuList pool = ipallocator.list(idAllocator._pool_);
            NavuContainer myPool = pool.elem(poolName);

            if (myPool == null) {
                // missing create
                System.err.println("pool missing, creating");
                pool.create(poolName);
                myPool = pool.elem(poolName);
            }

            cdbAllocations = myPool.list(idAllocator._allocation_);

            System.err.println("Adding existing allocations");

            for(NavuContainer alloc: cdbAllocations.elements()) {
                int id = (int) ((ConfUInt32) alloc.leaf(idAllocator._id_).
                                value()).longValue();
                String owner = alloc.leaf(idAllocator._owner_).value().
                    toString();
                System.err.println("Adding Allocation("+id+","+owner+")");
                super.add(new Allocation(id, owner));
            }
        }
        catch (Exception e) {
            LOGGER.error("Failed to setup up allocationsSet", e);
        }
    }

    public boolean add(Allocation e) {
        boolean res = super.add(e);

        if (res) {
            try {
                Integer id = e.getAllocated();

                NavuContainer alloc = cdbAllocations.create(id.toString());
                alloc.leaf("owner").set(new ConfBuf(e.getOccupant()));

                persistTrans(cdbAllocations);
            }
            catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }

        return res;
    }

    public boolean remove(Object o) {
        boolean res = super.remove(o);

        Allocation e = (Allocation) o;

        if (res) {
            try {
                Integer id = e.getAllocated();
                cdbAllocations.delete(id.toString());

                persistTrans(cdbAllocations);
            }
            catch (Exception ex ) {
                LOGGER.error("", ex);
            }

        }

        return res;
    }

    public void clear() {
        super.clear();

        try {
            for(NavuContainer sub: cdbAllocations.elements()) {
                cdbAllocations.delete(sub.getKey());
            }

            persistTrans(cdbAllocations);
        }
        catch (Exception ex ) {
            LOGGER.error("", ex);
        }
    }

    private void persistTrans(NavuNode node)
        throws NavuException, ConfException, IOException {
        NavuContext ctx = node.context();
        ctx.applyReplaceTrans();
    }

}
