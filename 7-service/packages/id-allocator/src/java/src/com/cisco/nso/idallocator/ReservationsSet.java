package com.cisco.nso.idallocator;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.cisco.nso.idallocator.namespaces.idAllocator;
import com.cisco.nso.idpool.Range;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfUInt32;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuContext;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;

class ReservationsSet extends HashSet<Range> {
    private static Logger LOGGER = Logger.getLogger(ReservationsSet.class);

    private String poolName;

    public NavuList cdbAvailables;

    public ReservationsSet(NavuContext operContext, String poolName) {
        this(operContext, poolName, idAllocator._reservations_);
    }

    public ReservationsSet(NavuContext operContext,
                           String poolName, String location) {
        super();
        this.poolName = poolName;

        try {
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer root = base.container(idAllocator.hash);
            NavuContainer idallocator = root.container(idAllocator.
                                                       prefix,
                                                       idAllocator.
                                                       _id_allocator_);
            NavuList pool = idallocator.list(idAllocator._pool_);
            NavuContainer myPool = pool.elem(poolName);

            if (myPool == null) {
                // missing create
                System.err.println("pool missing, creating");
                pool.create(poolName);
                myPool = pool.elem(poolName);
            }

            cdbAvailables = myPool.list(location);

            System.err.println("Adding existing availables");

            for(NavuContainer avail: cdbAvailables.elements()) {
                int start = (int) ((ConfUInt32) avail.leaf(idAllocator._start_).
                                   value()).longValue();
                int end = (int) ((ConfUInt32) avail.leaf(idAllocator._end_).
                                   value()).longValue();
                Range res = new Range(start,end);
                System.err.println("Adding Reservations("+res+")");
                super.add(res);
            }
        }
        catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public boolean add(Range ren) {
        boolean res = super.add(ren);

        if (res) {
            try {
                NavuContainer avail =
                    cdbAvailables.create(new String[]
                        {Integer.toString(ren.getStart()),
                         Integer.toString(ren.getEnd())});

                persistTrans(cdbAvailables);
            }
            catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }

        return res;
    }

    public boolean remove(Object o) {
        boolean res = super.remove(o);
        Range ren = (Range) o;

        if (res) {
            try {
                cdbAvailables.delete(new String[]
                    {Integer.toString(ren.getStart()),
                     Integer.toString(ren.getEnd())});

                persistTrans(cdbAvailables);
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
            for(NavuContainer ren: cdbAvailables.elements()) {
                cdbAvailables.delete(ren.getKey());
            }

            persistTrans(cdbAvailables);
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
