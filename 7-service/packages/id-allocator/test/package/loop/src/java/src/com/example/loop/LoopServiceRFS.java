/* Author: Johan Bevemyr <jbevemyr@cisco.com> */

package com.example.loop;


import java.util.Properties;

import com.example.loop.namespaces.loopService;
import com.cisco.resourcemanager.ResourceAllocator;
import com.cisco.resourcemanager.*;
import com.cisco.resourcemanager.namespaces.*;
import com.tailf.conf.ConfBool;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfKey;
import com.tailf.conf.ConfNamespace;
import com.tailf.conf.ConfObject;
import com.tailf.conf.ConfValue;
import com.tailf.conf.ConfPath;
import com.tailf.conf.ConfTag;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.ConfXMLParamValue;
import com.tailf.dp.DpActionTrans;
import com.tailf.dp.DpCallbackException;
import com.tailf.dp.annotations.ActionCallback;
import com.tailf.dp.annotations.ServiceCallback;
import com.tailf.dp.proto.ActionCBType;
import com.tailf.dp.proto.ServiceCBType;
import com.tailf.dp.services.ServiceContext;
import com.tailf.ncs.ns.Ncs;
import com.tailf.navu.*;
import com.tailf.cdb.*;
import com.tailf.maapi.*;
import com.tailf.conf.*;
import com.tailf.ncs.ResourceManager;
import com.tailf.ncs.annotations.Resource;
import com.tailf.ncs.annotations.ResourceType;
import com.tailf.ncs.annotations.Scope;

/**
 * This class implement the create logic for the Loop Service.
 *
 * The example will show how to use the resource allocator to allocate
 * an ip address, wait for it to become ready and then perform the
 * configuraton changes
 *
 *
 */

public class LoopServiceRFS {

    @Resource(type=ResourceType.CDB, scope=Scope.CONTEXT, qualifier="reactive")
    public  Cdb cdb;
    public  CdbSession cdbSess;

    /**
     * Create callback method.
     * This method is called when a service instance committed due to a create
     * or update event.
     *
     * This method returns a opaque as a Properties object that can be loop.
     * If not loop it is stored persistently by Ncs.
     * This object is then delivered as argument to new calls of the create
     * method for this service (fastmap algorithm).
     * This way the user can store and later modify persistent data outside
     * the service model that might be needed.
     *
     * @param context - The current ServiceContext object
     * @param service - The NavuNode references the service node.
     * @param root    - This NavuNode references the ncs root.
     * @param opaque  - Parameter contains a Properties object.
     *                  This object may be used to transfer
     *                  additional information between consecutive
     *                  calls to the create callback.  It is always
     *                  loop in the first call. I.e. when the service
     *                  is first created.
     * @return Properties the returning opaque instance
     * @throws DpCallbackException
     */
    @ServiceCallback(servicePoint = "loopspnt",
        callType = ServiceCBType.CREATE)
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
            ResourceAllocator.idRequest(service, root, poolName, serviceName);

            try {
                // Check if resource has been allocated, the allocated
                // id will be returned from the function and we can
                // get it from "id" in this case.
                ConfUInt32 id =
                    ResourceAllocator.idRead(cdb, poolName, serviceName);

                System.err.println("We got the id: "+id.longValue());

                // now read out some other dummy parameter in our service
                ConfBuf unit = (ConfBuf) loop.leaf("unit").value();

                // create some dummy config
                root.container("ncs","devices").
                    list("device").
                    elem(new ConfKey(devName)).
                    container("config").
                    container("ios","interface").list("Loopback").
                    sharedCreate(new ConfKey(unit));
            } catch (ResourceWaitException e) {
                // done, wait for reactive-re-deploy
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
}
