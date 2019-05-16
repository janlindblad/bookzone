package com.cisco.nso.idallocator;

import org.apache.log4j.Logger;

import com.cisco.nso.idallocator.namespaces.idAllocator;
import com.tailf.navu.NavuContext;

class AvailablesSet extends ReservationsSet {
    private static Logger LOGGER = Logger.getLogger(ReservationsSet.class);

    public AvailablesSet(NavuContext operContext,
                         String poolName) {
        super(operContext, poolName, idAllocator._available_);
    }
}
