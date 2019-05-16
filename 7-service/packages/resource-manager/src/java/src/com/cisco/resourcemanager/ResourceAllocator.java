/* Author: Johan Bevemyr <jbevemyr@cisco.com> */

package com.cisco.resourcemanager;

import com.cisco.resourcemanager.namespaces.resourceAllocator;
import com.tailf.conf.Conf;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfIPPrefix;
import com.tailf.conf.ConfObjectRef;
import com.tailf.conf.ConfPath;
import com.tailf.conf.ConfUInt32;
import com.tailf.conf.ConfUInt8;
import com.tailf.maapi.Maapi;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuContext;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuLeaf;
import com.tailf.navu.NavuList;
import com.tailf.navu.NavuNode;

public class ResourceAllocator {

    /**
     * Create a ip subnet allocation request.
     *
     * @param service  - The NavuNode references the service node.
     * @param root     - This NavuNode references the ncs root.
     * @param poolName - Name of pool to request from
     * @param cidrmask - CIDR mask length of requested subnet
     * @param id       - Unique allocation id
     */
    public static void subnetRequest(NavuNode service, NavuNode root,
                                     String poolName,
                                     int cidrmask, String id)
        throws NavuException, ConfException, ResourceException {
        NavuContainer rroot =
            root.getParent().container(resourceAllocator.hash);

        NavuContainer resourcePool =
            rroot.container(resourceAllocator.prefix,
                            resourceAllocator._resource_pools_);

        NavuList ipPool =
            resourcePool.list(resourceAllocator._ip_address_pool_);

        NavuContainer pool = ipPool.elem(poolName);

        if (pool == null)
            throw new ResourceException("Pool does not exist");

        NavuList allocation =
            pool.list(resourceAllocator._allocation_);

        NavuContainer myAlloc = allocation.sharedCreate(id);

        NavuContainer request =
            myAlloc.container(resourceAllocator._request_);

        NavuLeaf subnetSize =
            request.leaf(resourceAllocator._subnet_size_);

        subnetSize.sharedSet(new ConfUInt8(cidrmask));

        myAlloc.leaf(resourceAllocator._allocating_service).
            sharedSet(new ConfObjectRef(new ConfPath(service.getKeyPath())));

    }

    /**
     * Check if response is ready
     *
     * @param cdb      - A Cdb resource
     * @param poolName - Name of pool the request was created in
     * @param id       - Unique allocation id
     */
    public static boolean subnetReady(Maapi maapi, String poolName, String id)
        throws ResourceException, NavuException, ConfException {

        NavuContext operContext = null;
        try {
            // Setup CDB context, see resource allocation above
            operContext = new NavuContext(maapi);
            operContext.startOperationalTrans(Conf.MODE_READ);
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer operRoot = base.container(resourceAllocator.hash);

            NavuContainer pool =
                operRoot.container(resourceAllocator._resource_pools_).
                list(resourceAllocator._ip_address_pool_).
                elem(poolName);

            if (pool == null)
                throw new ResourceException("Pool does not exist");

            NavuContainer cdbAlloc =
                pool.list(resourceAllocator._allocation_).
                elem(id);

            if (cdbAlloc != null) {
                NavuLeaf subnet =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._subnet_);
                NavuLeaf error =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._error_);
                if (subnet != null && subnet.value() != null) {
                    return true;
                }
                else if (error != null && error.value() != null) {
                    return true;
                }
                else
                    return false;
            }
            else {
                return false;
            }
        } finally {
            operContext.finishClearTrans();
        }
    }

    /**
     * Read result of an ip subnet allocation request.
     *
     * @param cdb      - A Cdb resource
     * @param poolName - Name of pool the request was created in
     * @param id       - Unique allocation id
     */
    public static ConfIPPrefix subnetRead(Maapi maapi,
                                          String poolName,
                                          String id)
        throws ResourceException, NavuException, ConfException {

        NavuContext operContext = null;
        try {
            // Setup CDB context, see resource allocation above
            operContext = new NavuContext(maapi);
            operContext.startOperationalTrans(Conf.MODE_READ);
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer cdbroot = base.container(resourceAllocator.hash);

            NavuContainer pool =
                cdbroot.container(resourceAllocator._resource_pools_).
                list(resourceAllocator._ip_address_pool_).
                elem(poolName);

            if (pool == null)
                throw new ResourceException("Pool does not exist");

            NavuContainer cdbAlloc =
                pool.list(resourceAllocator._allocation_).
                elem(id);

            if (cdbAlloc != null) {
                NavuLeaf subnet =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._subnet_);
                NavuLeaf error =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._error_);
                if (subnet != null && subnet.value() != null) {
                    return (ConfIPPrefix) subnet.value();
                }
                else if (error != null && error.value() != null) {
                    throw new ResourceErrorException(error.value().toString());
                }
                else
                    throw new ResourceWaitException("not ready");
            }
            else {
                throw new ResourceWaitException("not ready");
            }
        } finally {
            operContext.finishClearTrans();
        }
    }

    /**
     * Read from which subnet an allocation was allocated
     *
     * @param cdb      - A Cdb resource
     * @param poolName - Name of pool the request was created in
     * @param id       - Unique allocation id
     */
    public static ConfIPPrefix fromRead(Maapi maapi,
                                        String poolName,
                                        String id)
        throws ResourceException, NavuException, ConfException {
        NavuContext operContext = null;

        try {
            // Setup CDB context, see resource allocation above
            operContext = new NavuContext(maapi);
            operContext.startOperationalTrans(Conf.MODE_READ);
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer cdbroot = base.container(resourceAllocator.hash);

            NavuContainer pool =
                cdbroot.container(resourceAllocator._resource_pools_).
                list(resourceAllocator._ip_address_pool_).
                elem(poolName);

            if (pool == null)
                throw new ResourceException("Pool does not exist");

            NavuContainer cdbAlloc =
                pool.list(resourceAllocator._allocation_).
                elem(id);

            if (cdbAlloc != null) {
                NavuLeaf subnet =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._from_);
                NavuLeaf error =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._error_);
                if (subnet != null && subnet.value() != null) {
                    return (ConfIPPrefix) subnet.value();
                }
                else if (error != null && error.value() != null) {
                    throw new ResourceErrorException(error.value().toString());
                }
                else
                    throw new ResourceWaitException("not ready");
            }
            else {
                throw new ResourceWaitException("not ready");
            }
        } finally {
            operContext.finishClearTrans();
        }
    }


    /**
     * Create a id  allocation request.
     *
     * @param service  - The NavuNode references the service node.
     * @param root     - This NavuNode references the ncs root.
     * @param poolName - Name of pool to request from
     * @param id       - Unique allocation id
     */
    public static void idRequest(NavuNode service, NavuNode root,
                                     String poolName, String id)
        throws NavuException, ConfException, ResourceException {
        NavuContainer rroot =
            root.getParent().container(resourceAllocator.hash);

        NavuContainer resourcePool =
            rroot.container(resourceAllocator.prefix,
                            resourceAllocator._resource_pools_);

        NavuList idPool =
            resourcePool.list(resourceAllocator._id_pool_);

        NavuContainer pool = idPool.elem(poolName);

        if (pool == null)
            throw new ResourceException("Pool does not exist");

        NavuList allocation =
            pool.list(resourceAllocator._allocation_);

        NavuContainer myAlloc = allocation.sharedCreate(id);

        myAlloc.leaf(resourceAllocator._allocating_service).
            sharedSet(new ConfObjectRef(new ConfPath(service.getKeyPath())));

    }

    /**
     * Check if id is ready
     *
     * @param cdb      - A Cdb resource
     * @param poolName - Name of pool the request was created in
     * @param id       - Unique allocation id
     */
    public static boolean idReady(Maapi maapi, String poolName, String id)
        throws ResourceException, NavuException, ConfException {
        NavuContext operContext = null;

        try {
            // Setup CDB context, see resource allocation above
            operContext = new NavuContext(maapi);
            operContext.startOperationalTrans(Conf.MODE_READ);
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer cdbroot = base.container(resourceAllocator.hash);

            NavuContainer pool =
                cdbroot.container(resourceAllocator._resource_pools_).
                list(resourceAllocator._id_pool_).
                elem(poolName);

            if (pool == null)
                throw new ResourceException("Pool does not exist");

            NavuContainer cdbAlloc =
                pool.list(resourceAllocator._allocation_).
                elem(id);

            if (cdbAlloc != null) {
                NavuLeaf resId =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._id_);
                NavuLeaf error =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._error_);
                if (resId != null && resId.value() != null ) {
                    return true;
                }
                else if (error != null && error.value() != null) {
                    return true;
                }
                else
                    return false;
            }
            else {
                return false;
            }
        } finally {
            operContext.finishClearTrans();
        }
    }

    /**
     * Read result of an id allocation request.
     *
     * @param cdb      - A Cdb resource
     * @param poolName - Name of pool the request was created in
     * @param id       - Unique allocation id
     */
    public static ConfUInt32 idRead(Maapi maapi, String poolName, String id)
        throws ResourceException, NavuException, ConfException {
        NavuContext operContext = null;

        try {
            // Setup CDB context, see resource allocation above
            operContext = new NavuContext(maapi);
            operContext.startOperationalTrans(Conf.MODE_READ);
            NavuContainer base = new NavuContainer(operContext);
            NavuContainer cdbroot = base.container(resourceAllocator.hash);

            NavuContainer pool =
                cdbroot.container(resourceAllocator._resource_pools_).
                list(resourceAllocator._id_pool_).
                elem(poolName);

            if (pool == null)
                throw new ResourceException("Pool does not exist");

            NavuContainer cdbAlloc =
                pool.list(resourceAllocator._allocation_).
                elem(id);

            if (cdbAlloc != null) {
                NavuLeaf resId =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._id_);
                NavuLeaf error =
                    cdbAlloc.container(resourceAllocator._response_).
                    leaf(resourceAllocator._error_);
                if (resId != null && resId.value() != null ) {
                    return (ConfUInt32) resId.value();
                }
                else if (error != null && error.value() != null) {
                    throw new ResourceErrorException(error.value().toString());
                }
                else
                    throw new ResourceWaitException("not ready");
            }
            else {
                throw new ResourceWaitException("not ready");
            }
        } finally {
            operContext.finishClearTrans();
        }
    }
}
