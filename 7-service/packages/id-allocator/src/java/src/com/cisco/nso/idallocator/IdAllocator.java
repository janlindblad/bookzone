package com.cisco.nso.idallocator;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cisco.nso.idallocator.namespaces.idAllocator;
import com.cisco.nso.idpool.Allocation;
import com.cisco.nso.idpool.IDPool;
import com.cisco.nso.idpool.Range;
import com.cisco.nso.idpool.exceptions.AllocationException;
import com.cisco.nso.idpool.exceptions.InvalidRangeException;
import com.cisco.nso.idpool.exceptions.PoolExhaustedException;
import com.cisco.resourcemanager.namespaces.resourceAllocator;
import com.tailf.cdb.Cdb;
import com.tailf.cdb.CdbDBType;
import com.tailf.cdb.CdbDiffIterate;
import com.tailf.cdb.CdbException;
import com.tailf.cdb.CdbSession;
import com.tailf.cdb.CdbSubscription;
import com.tailf.cdb.CdbSubscriptionSyncType;
import com.tailf.conf.Conf;
import com.tailf.conf.ConfBool;
import com.tailf.conf.ConfBuf;
import com.tailf.conf.ConfEnumeration;
import com.tailf.conf.ConfException;
import com.tailf.conf.ConfInt32;
import com.tailf.conf.ConfKey;
import com.tailf.conf.ConfObject;
import com.tailf.conf.ConfObjectRef;
import com.tailf.conf.ConfPath;
import com.tailf.conf.ConfTag;
import com.tailf.conf.ConfUInt32;
import com.tailf.conf.ConfValue;
import com.tailf.conf.ConfXMLParam;
import com.tailf.conf.DiffIterateFlags;
import com.tailf.conf.DiffIterateOperFlag;
import com.tailf.conf.DiffIterateResultFlag;
import com.tailf.maapi.Maapi;
import com.tailf.maapi.MaapiUserSessionFlag;
import com.tailf.navu.NavuContainer;
import com.tailf.navu.NavuContext;
import com.tailf.navu.NavuException;
import com.tailf.navu.NavuList;
import com.tailf.ncs.ApplicationComponent;
import com.tailf.ncs.NcsMain;
import com.tailf.ncs.ResourceManager;
import com.tailf.ncs.annotations.Resource;
import com.tailf.ncs.annotations.ResourceType;
import com.tailf.ncs.annotations.Scope;


public class IdAllocator implements ApplicationComponent {
    private static Logger LOGGER = Logger.getLogger(IdAllocator.class);

    private CdbSubscription sub = null;
    private CdbSession wsess, isess;

    private Set<Pool> pools = new HashSet<Pool>();

    public IdAllocator() {
    }

    @Resource(type=ResourceType.CDB, scope=Scope.INSTANCE,
              qualifier="id-allocator-subscriber")
    private Cdb cdb;

    @Resource(type=ResourceType.CDB, scope=Scope.INSTANCE,
              qualifier="id-allocator-reactive-fm-loop")
    private Cdb wcdb;

    @Resource(type=ResourceType.CDB, scope=Scope.INSTANCE,
              qualifier="id-allocator-reactive-fm-loop-iter")
    private Cdb icdb;

    @Resource(type=ResourceType.MAAPI, scope=Scope.INSTANCE,
              qualifier="id-alloc-reactive-fm-idallocator-m")
    private Maapi maapi;

    private int tid;
    private int alloc_subid, range_subid, exclude_subid, pool_subid;

    private NavuList idpool;

    private boolean isMaster = true;

    public void init() {
        try {
            wsess = wcdb.startSession(CdbDBType.CDB_OPERATIONAL);
            // system session, either we must pick up the NB ussername through
            // the fastmap data, or we must have a dedicated user that is
            // allowed
            // to do this. Authgroup and credentials are needed to to redeploy
            // since that might touch the network.
            maapi.startUserSession("admin",
                                   maapi.getSocket().getInetAddress(),
                                   "system",
                                   new String[] {"admin"},
                                   MaapiUserSessionFlag.PROTO_TCP);

            tid = maapi.startTrans(Conf.DB_RUNNING, Conf.MODE_READ);

            sub = cdb.newSubscription();

            // create subscriptions
            alloc_subid = sub.subscribe(
                     3, new resourceAllocator(),
                     "/"+
                     resourceAllocator.prefix+":"+
                     resourceAllocator._resource_pools_+"/"+
                     resourceAllocator._id_pool_+"/"+
                     resourceAllocator._allocation_);

            range_subid = sub.subscribe(
                     2, new resourceAllocator(),
                     "/"+
                     resourceAllocator.prefix+":"+
                     resourceAllocator._resource_pools_+"/"+
                     resourceAllocator._id_pool_+"/"+
                     idAllocator._range_);

            exclude_subid = sub.subscribe(
                     2, new resourceAllocator(),
                     "/"+
                     resourceAllocator.prefix+":"+
                     resourceAllocator._resource_pools_+"/"+
                     resourceAllocator._id_pool_+"/"+
                     idAllocator._exclude_);

            pool_subid = sub.subscribe(
                     1, new resourceAllocator(),
                     "/"+
                     resourceAllocator.prefix+":"+
                     resourceAllocator._resource_pools_+"/"+
                     resourceAllocator._id_pool_);

            // tell CDB we are ready for notifications
            sub.subscribeDone();

            loadState();
        }
        catch (Exception e) {
            LOGGER.error("", e);
        }
    }


    private void loadState() throws NavuException, UnknownHostException,
                                    InvalidRangeException {
        pools = new HashSet<Pool>();

        // ************************************************************
        // read existing config and create existing pools
        //

        NavuContext context = new NavuContext(maapi, tid);
        NavuContainer base = new NavuContainer(context);
        NavuContainer root = base.container(resourceAllocator.hash);
        NavuContainer resources = root.container(resourceAllocator.prefix,
                                                 resourceAllocator.
                                                 _resource_pools_);
        idpool = resources.list(resourceAllocator._id_pool_);

        // Create id pools
        for(NavuContainer pool: idpool.elements()) {
            createPool(pool);
        }

        ArrayList<String> init_redeps = new ArrayList<String>();

        try {
            System.err.println("pool size = "+pools.size());
            for(Pool pool: pools) {
                System.err.println("examining pool: "+pool.path);
                NavuList allocList =
                    (NavuList) idpool.getNavuNode(
                        new ConfPath(pool.path+"/"+resourceAllocator.
                                     _allocation_));
                for(NavuContainer alloc: allocList.elements()) {
                    ConfPath path = new ConfPath(alloc.getKeyPath());

                    try {
                        wsess.getCase(
                            resourceAllocator._response_choice_,
                            path+"/"+resourceAllocator._response_);
                    } catch (ConfException e) {
                        // no case set, continue
                        System.err.println("missing allocation for: "+
                                           path);
                        Request req = new Request();
                        ConfPath poolPath = new ConfPath(pool.path);
                        req.path = path;
                        req.pool = null;
                        req.val = null;
                        req.t = Type.ALLOC;
                        try {
                            allocateId(pool, init_redeps, req);
                        } catch (Exception ex) {
                            LOGGER.error("", ex);
                        }
                    }
                }
            }
        } catch (ConfException e) {
            System.err.println("error 1");
            LOGGER.error("", e);
        } catch (IOException e) {
            System.err.println("error 2");
            LOGGER.error("", e);
        } catch (Exception e) {
            System.err.println("error 3");
            LOGGER.error("", e);
        } catch (Throwable e) {
            System.err.println("error 4");
            LOGGER.error("", e);
        }

        // invoke redeploy
        for (String rep : init_redeps) {
            System.err.println("Redeploying: "+rep);
            redeploy(rep);
        }
    }

    public void run() {

        // FIXME: we should check for pending allocations at this point
        // since the system may have crashed before processing the
        // allocation request

        while(true) {
            int[] points;
            try {
                points = sub.read();
            } catch (Exception e) {
                if (e.getCause() instanceof java.io.EOFException) {
                    // silence here, normal close (redeploy/reload package)
                    ;
                }
                else {
                    LOGGER.error("",e );
                }
                return;
            }
            try {
                boolean ha_mode_exists =
                    maapi.exists(tid, "/tfnm:ncs-state/ha");

                if (ha_mode_exists) {
                    ConfEnumeration ha_mode_enum =  (ConfEnumeration)
                        maapi.getElem(tid, "/tfnm:ncs-state/ha/mode");

                    String ha_mode =
                        ConfEnumeration.getLabelByEnum(
                               "/tfnm:ncs-state/ha/mode",
                               ha_mode_enum);

                    if (!("none".equals(ha_mode) ||
                          "normal".equals(ha_mode) ||
                          "master".equals(ha_mode))) {
                        // slave or relay-slave
                        sub.sync(CdbSubscriptionSyncType.DONE_PRIORITY);
                        isMaster = false;
                        continue;
                    }
                    else {
                        if (isMaster == false) {
                            // we just became master, we need to re-read
                            // our state
                            loadState();
                            isMaster = true;
                        }

                    }
                }
            } catch (IOException e) {
                LOGGER.error("",e );
            } catch (ConfException e) {
                LOGGER.error("",e );
            } catch (InvalidRangeException e) {
                LOGGER.error("",e );
            }

            try {
                isess = icdb.startSession(CdbDBType.CDB_RUNNING);

                ArrayList<Request> reqs = new ArrayList<Request>();
                EnumSet<DiffIterateFlags> enumSet =
                    EnumSet.<DiffIterateFlags>of(
                                    DiffIterateFlags.ITER_WANT_PREV,
                                    DiffIterateFlags.ITER_WANT_SCHEMA_ORDER);

                // process each subscription point
                for(int i=0 ; i < points.length ; i++) {
                    if (points[i] == alloc_subid) {
                        try {
                            sub.diffIterate(points[i],
                                            new Iter(sub, Type.ALLOC),
                                            enumSet, reqs);
                        }
                        catch (Exception e) {
                            reqs = null;
                        }
                    }
                    else if (points[i] == pool_subid) {
                        try {
                            sub.diffIterate(points[i],
                                            new Iter(sub, Type.POOL),
                                            enumSet, reqs);
                        }
                        catch (Exception e) {
                            reqs = null;
                        }
                    }
                    else if (points[i] == range_subid) {
                        try {
                            sub.diffIterate(points[i],
                                            new Iter(sub, Type.RANGE),
                                            enumSet, reqs);
                        }
                        catch (Exception e) {
                            reqs = null;
                        }
                    }
                    else if (points[i] == exclude_subid) {
                        try {
                            sub.diffIterate(points[i],
                                            new Iter(sub, Type.EXCLUDE),
                                            enumSet, reqs);
                        }
                        catch (Exception e) {
                            reqs = null;
                        }
                    }
                }

                isess.endSession();

                ArrayList<String> redeps = new ArrayList<String>();
                ArrayList<Pool> modifiedPools = new ArrayList<Pool>();

                // System.err.println("reqs.length="+reqs.size());

                // If we are calling an external allocator we should do
                // the following call here and not after the for loop
                // sub.sync(CdbSubscriptionSyncType.DONE_PRIORITY);

                for (Request req : reqs) {

                    // Find proper pool
                    Pool p = null;

                    for(Pool pool: pools) {
                        if (pool.p.getName().equals(req.pool.elementAt(0).
                                                    toString())) {
                            p = pool;
                            break;
                        }
                    }

                    if (p == null &&
                        !(req.t == Type.POOL && req.op == Operation.CREATE)) {
                        LOGGER.error("No matching pool found: "+
                                     req.pool.elementAt(0).toString());
                        System.err.println("No matching pool found!");
                        continue;
                    }

                    if (req.t == Type.POOL) {
                        if (req.op == Operation.CREATE) {
                            // A new pool has been added
                            try {
                                createPool(idpool.elem(req.pool));
                            }
                            catch (Exception e) {
                                LOGGER.error("Failed to create pool", e);
                            }
                        }
                        else {
                            // An existing pool has been removed, cleanup
                            try {
                                pools.remove(p);
                                // Delete CDB oper structures for pool
                                NavuContainer myPool =
                                    ((NavuContainer) p.availables.
                                     cdbAvailables.getParent());
                                ((NavuList) myPool.getParent()).
                                    delete(myPool.getKey());
                            }
                            catch (Exception e) {
                                LOGGER.error("Failed to delete pool", e);
                            }
                        }
                    }
                    else if (req.t == Type.RANGE) {
                        LOGGER.debug("got range change");

                        modifiedPools.add(p);

                        NavuContainer cdbRange =
                            ((NavuContainer) p.availables.cdbAvailables.
                             getParent()).container(idAllocator._range_);

                        int start = req.range_start;
                        int end = req.range_end;
                        Range range = new Range(start, end);

                        // The range has been modified Remove all
                        // allocations that are outside the new range,
                        // allocate ids, and reactive-re-deploy all services.

                        ArrayList<Request> reallocReqs =
                            new ArrayList<Request>();

                        reallocateIds(p, cdbRange, reallocReqs, range, false);

                        // now all ids should have been released
                        // and we can modify the range

                        try {
                            System.err.println("setting new range: "+range);
                            p.p.setRange(range);
                        } catch (Exception e) {
                            // ignore for now
                            LOGGER.error("",e );
                        }

                        for(Request reallocReq: reallocReqs)
                            allocateId(p, redeps, reallocReq);
                    }
                    else if (req.t == Type.EXCLUDE) {

                        modifiedPools.add(p);

                        NavuList cdbExclude =
                            ((NavuContainer) p.availables.cdbAvailables.
                             getParent()).list(idAllocator._exclude_);
                        NavuContainer cdbRange =
                            ((NavuContainer) p.availables.cdbAvailables.
                             getParent()).container(idAllocator._range_);
                        LOGGER.debug("ex req.key="+req.key);
                        int start = (int)
                            ((ConfUInt32)req.key.elementAt(0)).longValue();
                        int end = (int)
                            ((ConfUInt32)req.key.elementAt(1)).longValue();
                        String[] key = new String[] {
                            Integer.toString(start), Integer.toString(end)};
                        Range range = new Range(start, end);

                        if (req.op == Operation.DELETE) {
                            // A exclusion has been removed to the pool
                            if (cdbExclude.elem(key) != null) {
                                p.p.removeFromReservations(range);
                                cdbExclude.delete(key);
                            }
                            else {
                                System.err.println("already removed: "+key);
                            }
                        }
                        else {
                            // A new exclusion has been added to the pool
                            if (cdbExclude.elem(key) == null) {
                                System.err.println("new exclusion");
                                // FIXME: remove all allocations that
                                // belong to this range, allocate new
                                // ids, and reactive-re-deploy all services.

                                ArrayList<Request> reallocReqs =
                                    new ArrayList<Request>();

                                reallocateIds(p, cdbRange, reallocReqs, range,
                                              true);

                                // now all ids in the new excluded section
                                // should have been released
                                // and we can add the new range exclusion

                                cdbExclude.create(key);

                                try {
                                    p.p.addToReservations(range);
                                } catch (Exception e) {
                                    // ignore for now
                                    LOGGER.error("",e );
                                }

                                for(Request reallocReq: reallocReqs)
                                    allocateId(p, redeps, reallocReq);

                            } else {
                                System.err.println("already removed: "+key);
                            }
                        }
                    }
                    else if (req.t == Type.ALLOC) {
                        if (req.op == Operation.CREATE) {
                            allocateId(p, redeps, req);
                        }
                        else {
                            // delete
                            // clean up oper data, and de-allocate
                            try {
                                if (req.val != null) {
                                    int id = (int) ((ConfUInt32) req.val).
                                        longValue();
                                    p.p.release(id);
                                }

                                cleanupResponse(req.path.toString());
                            }
                            // No we didn't
                            catch (Exception e) {
                                LOGGER.error("",e );
                            }
                        }
                    }

                    if (p != null) {
                        if (p.reservations.cdbAvailables != null)
                            p.reservations.cdbAvailables.stopCdbSession();

                        if (p.allocations.cdbAllocations != null)
                            p.allocations.cdbAllocations.stopCdbSession();

                        if (p.availables.cdbAvailables != null)
                            p.availables.cdbAvailables.stopCdbSession();
                    }
                }

                NavuContext context = new NavuContext(maapi, tid);
                NavuContainer base = new NavuContainer(context);
                NavuContainer root = base.container(resourceAllocator.hash);
                NavuContainer resources = root.container(
                    resourceAllocator.prefix,resourceAllocator.
                    _resource_pools_);

                for (Pool p : modifiedPools) {
                    // The pool definition was changed, see if some
                    // previously failed allocation should now be retried
                    NavuContainer idPool =
                        resources.list(resourceAllocator._id_pool_).
                        elem(p.p.getName());
                    NavuList allocations =
                        idPool.list(resourceAllocator._allocation_);

                    for(NavuContainer alloc : allocations.elements()) {
                        String cdbAllocPath =
                            new ConfPath(alloc.getKeyPath()).toString();
                        String responsePath =
                            cdbAllocPath+"/"+resourceAllocator._response_;

                        ConfTag selectedCase = null;

                        try {
                            selectedCase =
                                ((ConfTag) wsess.getCase(
                                    resourceAllocator.
                                    _response_choice_, responsePath));
                        } catch (ConfException e) {
                            // no case selected, ignore
                            continue;
                        }

                        if (selectedCase != null &&
                            selectedCase.getTag().equals(
                                resourceAllocator._error_)) {
                            // previously failed allocation, retry
                            Request r = new Request();
                            r.path = new ConfPath(alloc.getKeyPath());
                            cleanupResponse(r.path.toString());
                            allocateId(p, redeps, r);
                        }
                    }
                }

                // invoke redeploy
                for (String rep : redeps) {
                    System.err.println("Redeploying: "+rep);
                    redeploy(rep);
                }

            }
            catch (ConfException e) {
                if (e.getCause() instanceof java.io.EOFException) {
                    // silence here, normal close (redeploy/reload package)
                    LOGGER.error("",e );
                    ;
                }
                else {
                    LOGGER.error("",e );
                }
            }
            catch (SocketException e) {
                // silence here, normal close (redeploy/reload package)
                LOGGER.error("",e );
                ;
            }
            catch (ClosedChannelException e) {
                // silence here, normal close (redeploy/reload package)
                LOGGER.error("",e );
                ;
            }
            catch (Exception e) {
                LOGGER.error("",e );
            }
            catch (Throwable e) {
                LOGGER.error("", e);
            }
            try {
                // It's important that we return as early as possible her,
                // This is a common technique, gather the work to do, tell
                // CDB that we're done and then do the work.
                // It could be that the allocator needs to reach out (RPC)
                // and that can be slow

                // NOTE: if you are calling an external algorithm you must
                // do the below call earlier
                sub.sync(CdbSubscriptionSyncType.DONE_PRIORITY);
            }
            catch (Exception e) {
                LOGGER.error("",e );
            }
        }
    }

    private String getOwner(String path) throws Exception {
        if (maapi.exists(
                tid, path+"/"+resourceAllocator._allocating_service_)) {
            ConfObjectRef v =
                (ConfObjectRef) maapi.
                getElem(tid, path+"/"+resourceAllocator._allocating_service_);

            return new ConfPath(v.getElems()).toString();
        }
        else
            return "";
    }

    private int getAllocatedId(String path) throws Exception {
        // check if it has allocation
        try {
            String selectedCase =
                wsess.getCase(
                    resourceAllocator._response_choice_,
                    path+"/"+resourceAllocator._response_).
                toString();

            System.err.println(
                "found selected case: "+selectedCase);

            if ("ralloc:ok".equals(selectedCase)) {
                int allocatedId =
                    (int) ((ConfUInt32) wsess.getElem(
                               path + "/"+
                               resourceAllocator._response_+"/"+
                               resourceAllocator._id_)).longValue();
                System.err.println("found allocated id: "+
                                   allocatedId);
                return allocatedId;
            }
            else
                return -1;
        } catch (ConfException e) {
            System.err.println("no case selected for: "+path);
            return -1;
        }
    }

    private boolean getSync(String path) throws Exception {
        return ((ConfBool) maapi.getElem(
                    tid, path+"/"+resourceAllocator.
                    _request_+"/"+idAllocator._sync_)).booleanValue();
    }

    private int getRequestId(String path) throws Exception {
        return ((ConfInt32) maapi.getElem(
                    tid, path+"/"+resourceAllocator.
                    _request_+"/"+idAllocator._id_)).intValue();
    }

    private void cleanupResponse(String path) throws Exception {
        wsess.setCase(resourceAllocator._response_choice_,
                      null,
                      path+"/"+resourceAllocator._response_);
        try {
            wsess.delete(path+"/"+resourceAllocator._response_+"/"+
                         resourceAllocator._id_);
        } catch (CdbException e) {
            // ignore
            ;
        }
        try {
            wsess.delete(path+"/"+resourceAllocator._response_+"/"+
                         resourceAllocator._error_);
        } catch (CdbException e) {
            // ignore
            ;
        }
    }

    private void reportSuccess(int id, String path,
                               ArrayList<String> redeps)
        throws Exception {
        System.out.println("SET: "+ path+"/"+resourceAllocator._response_+"/"+
                           resourceAllocator._id_+" -> " + id);
        wsess.setElem(new ConfUInt32(id),
                      path+"/"+resourceAllocator._response_+"/"+
                      resourceAllocator._id_);
        /* we need to setCase after setElem due to a bug in NCS */
        wsess.setCase(resourceAllocator._response_choice_,
                      resourceAllocator._ok_,
                      path+"/"+resourceAllocator._response_);

        String owner = getOwner(path);

        if (owner != "") {
            // redeploy the service that consumes this
            // data, runs in separate thread FIXME:
            // rewrite to only redeploy each service
            // once if it allocates multiple addresses

            if (!redeps.contains(owner)) {
                System.err.println("adding "+owner+
                                   " to redeploy list");
                redeps.add(owner);
            }
        }
    }

    private void reportError(String error, String path,
                             ArrayList<String> redeps)
        throws Exception {
        System.out.println("SET: " + path + "/response/error ->" +
                           error);
        wsess.setElem(new ConfBuf(error),
                      path+"/"+
                      resourceAllocator._response_+"/"+
                      resourceAllocator._error_);
        /* we need to setCase after setElem due to a bug in NCS */
        wsess.setCase(resourceAllocator._response_choice_,
                      resourceAllocator._error_,
                      path+"/"+resourceAllocator._response_);

        String owner = getOwner(path);

        if (owner != "") {
            // redeploy the service that consumes this
            // data, runs in separate thread FIXME:
            // rewrite to only redeploy each service
            // once if it allocates multiple addresses

            if (!redeps.contains(owner)) {
                System.err.println("adding "+owner+
                                   " to redeploy list");
                redeps.add(owner);
            }
        }
    }

    private void allocateOneId(Pool p, ArrayList<String> redeps, Request req,
                               int requestedId)
        throws Exception, NavuException {

        try {
            Allocation a;

            String owner = getOwner(req.path.toString());

            if (requestedId == -1)
                a = p.p.allocate(owner);
            else
                a = p.p.allocate(owner, requestedId);

            // Write the result and redeploy
            int id = a.getAllocated();

            reportSuccess(id, req.path.toString(), redeps);
        }
        catch (AllocationException ex) {
            reportError(ex.toString(), req.path.toString(), redeps);
        }
    }

    private void allocateId(Pool p, ArrayList<String> redeps, Request req)
        throws Exception, NavuException {

        boolean sync = getSync(req.path.toString());

        if (!sync) {
            int requestedId = getRequestId(req.path.toString());
            allocateOneId(p, redeps, req, requestedId);
        }
        else {
            // check if we have already produced a response for this
            // allocation due to processing another element in the
            // sync group

            try {
                wsess.getCase(
                    resourceAllocator._response_choice_,
                    req.path+"/"+resourceAllocator._response_);
                // already processed, return
                return;
            } catch (ConfException e) {
                // no case set, continue
                ;
            }

            // we need to see if there already is an allocation with an
            // id, in which case we should requests the same id in
            // this pool
            Set<SyncGroup> syncGroup = new HashSet<SyncGroup>();

            int allocatedId = -1;
            int requestedId = getRequestId(req.path.toString());

            String allocationId =
                maapi.getElem(tid, req.path+"/"+resourceAllocator._id_).
                toString();

            syncGroup.add(new SyncGroup(p, req.path.toString()));

            for(Pool pool: pools) {
                if (pool == p)
                    continue;

                String path = pool.path+"/"+resourceAllocator.
                    _allocation_+"{"+allocationId+"}";

                if (maapi.exists(tid, path) && getSync(path) == true) {
                    System.out.println("found sync pool node: "+pool.path);
                    syncGroup.add(new SyncGroup(pool, path));


                    if (allocatedId == -1)
                        allocatedId = getAllocatedId(path);

                    if (requestedId == -1)
                        requestedId = getRequestId(path);
                    else if (getRequestId(path) != -1 &&
                             getRequestId(path) != requestedId) {
                        // We cannot request two different ids in the
                        // same group, throw an error
                        throw new PoolExhaustedException(
                            "Conflicting id requests");
                    }
                }
            }

            // We have three special cases
            // 1. only one node in sync group - allocate as normal
            if (syncGroup.size() == 1) {
                allocateOneId(p, redeps, req, requestedId);
            }
            // 2. at least one node has allocation already - try to allocate
            //    same id for this entry
            else if (allocatedId != -1) {
                allocateOneId(p, redeps, req, allocatedId);
            }
            // 3. at least one node has requested a specific id - try to
            //    allocate same id for this entry
            else if (requestedId != -1) {
                allocateOneId(p, redeps, req, requestedId);
            }
            // 4. no node has allocation - try finding one id that can be
            //    allocated in all pools
            else {
                // The way we do this is by allocating an id from the pool
                // and then trying to allocate the same id in all pools:
                //
                // - if this succeeds we are done
                // - if this fails, we save the allocated id in a Set and
                //   attempts to allocate a new id:
                //   - if this fails, we fail and release all allocations in Set
                //   - if this succeeds, we try to allocate same id in all pools
                //     - if this succeeds, we deallocate all failed allocations
                //       and keep the successful one
                //     - if it fails, we save the allocaiton in Set, and try a
                //       new allocation

                Set<Allocation> failedCandidates = new HashSet<Allocation>();

                while(true) {
                    try {
                        Allocation a = p.p.allocate(
                            getOwner(req.path.toString()));
                        int id = a.getAllocated();

                        Set<PoolAlloc> poolAllocs = new HashSet<PoolAlloc>();

                        try {
                            for(SyncGroup sg: syncGroup) {
                                if (sg.p == p) continue;
                                Allocation alloc = sg.p.p.allocate(
                                    getOwner(sg.path), id);
                                poolAllocs.add(new PoolAlloc(sg.p, alloc));
                            }

                            // success!
                            // release failed candidates
                            for(Allocation fa: failedCandidates) {
                                p.p.release(fa);
                            }

                            // report all successful allocations
                            for(SyncGroup sg: syncGroup) {
                                reportSuccess(id, sg.path, redeps);
                            }

                            break;
                        } catch (AllocationException ex) {
                            // Failed, release all and try another
                            for(PoolAlloc pa: poolAllocs) {
                                pa.p.p.release(pa.a);
                            }

                            failedCandidates.add(a);
                        }
                    } catch (AllocationException ex) {
                        // Failed, release all and report failure
                        // release failed candidates
                        for(Allocation fa: failedCandidates) {
                            p.p.release(fa);
                        }

                        // report all failed
                        for(SyncGroup sg: syncGroup) {
                            reportError("sync allocation failed",
                                        sg.path, redeps);
                        }

                        break;
                    }
                }
            }
        }
    }

    // Release all allocations that are in range and add them to
    // the reallocReqs list.
    private void reallocateIds(Pool p, NavuContainer cdbRange,
                               ArrayList<Request> reallocReqs,
                               Range range, boolean exclude)
        throws Exception {
        // Loop over all allocations in the pool and reallocate all that
        // fall outside the new range

        NavuContext context = new NavuContext(maapi, tid);
        NavuContainer base = new NavuContainer(context);
        NavuContainer root = base.container(resourceAllocator.hash);
        NavuContainer resources = root.container(resourceAllocator.prefix,
                                                 resourceAllocator.
                                                 _resource_pools_);
        NavuContainer idPool =
            resources.list(resourceAllocator._id_pool_).
            elem(p.p.getName());
        NavuList allocations = idPool.list(resourceAllocator._allocation_);

        for(NavuContainer alloc : allocations.elements()) {
            NavuContainer cdbAlloc =
                (NavuContainer) cdbRange.
                getNavuNode(new ConfPath(alloc.getKeyPath()));

            NavuContainer response =
                cdbAlloc.container(resourceAllocator._response_);
            if (response.getSelectedCase(resourceAllocator._response_choice_).
                getTag().equals(resourceAllocator._ok_)) {
                int id = (int) ((ConfUInt32) response.leaf(resourceAllocator.
                                                           _id_).
                                value()).longValue();
                System.err.println("Checking if "+id+" is in range "+range);
                if (exclude && range.contains(id)) {
                    // needs to be reallocated
                    response.leaf(resourceAllocator._id_).delete();
                    p.p.release(id);
                    Request r = new Request();
                    r.path = new ConfPath(alloc.getKeyPath());
                    reallocReqs.add(r);
                    cleanupResponse(r.path.toString());
                } else if (!exclude && !range.contains(id)) {
                    // needs to be reallocated
                    response.leaf(resourceAllocator._id_).delete();
                    p.p.release(id);
                    Request r = new Request();
                    r.path = new ConfPath(alloc.getKeyPath());
                    reallocReqs.add(r);
                    cleanupResponse(r.path.toString());
                }
            }
        }
    }

    public void finish() {
        System.err.println("finish start");
        try {
            wsess.endSession();
            for(Pool p: pools) {
                if (p.reservations.cdbAvailables != null)
                    p.reservations.cdbAvailables.context().finishClearTrans();

                if (p.allocations.cdbAllocations != null)
                    p.allocations.cdbAllocations.context().finishClearTrans();

                if (p.availables.cdbAvailables != null)
                    p.availables.cdbAvailables.context().finishClearTrans();
            }
        }
        catch (ClosedChannelException e) {
            // silence here, normal close (redeploy/reload package)
            ;
        }
        catch (Exception e) {
            LOGGER.error("",e );
        }
        try {
            try {
                maapi.finishTrans(tid);
            }
            catch (Throwable ignore) {}
            ResourceManager.unregisterResources(this);
        }
        catch (Exception e) {
            LOGGER.error("",e );
        }
        System.err.println("finish end");
    }

    private void createPool(NavuContainer pool)
        throws NavuException, UnknownHostException, InvalidRangeException {
        ReservationsSet reservations;
        AllocationsSet allocations;
        AvailablesSet availables;

        String pname = pool.leaf("name").value().toString();

        NavuContext reservContext = new NavuContext(maapi);
        reservContext.startOperationalTrans(Conf.MODE_READ_WRITE);
        reservations = new ReservationsSet(reservContext, pname);
        reservContext.applyReplaceTrans();

        NavuContext availContext = new NavuContext(maapi);
        availContext.startOperationalTrans(Conf.MODE_READ_WRITE);
        availables = new AvailablesSet(availContext, pname);
        availContext.applyReplaceTrans();

        NavuContext allocContext = new NavuContext(maapi);
        allocContext.startOperationalTrans(Conf.MODE_READ_WRITE);
        allocations = new AllocationsSet(allocContext, pname);
        allocContext.applyReplaceTrans();


        System.err.println("creating new pool");
        IDPool p = new IDPool(pname, reservations, availables, allocations);

        // configure overall range
        NavuContainer poolRange =
            pool.container(idAllocator.prefix, idAllocator._range_);

        int start = (int) ((ConfUInt32) poolRange.leaf(idAllocator._start_).
                           value()).longValue();
        int end =  (int) ((ConfUInt32) poolRange.leaf(idAllocator._end_).
                          value()).longValue();

        p.setRange(new Range(start,end));

        // compare configured excludes to known excludes and add/remove
        NavuList cdbExclude = ((NavuContainer) availables.cdbAvailables.
                               getParent()).list(idAllocator._exclude_);

        NavuList poolExclude = pool.list(idAllocator.prefix,
                                         idAllocator._exclude_);

        // first add those that are new
        for(NavuContainer exclude: poolExclude.elements()) {
            start = (int) ((ConfUInt32) exclude.leaf(idAllocator._start_).
                           value()).longValue();
            end =  (int) ((ConfUInt32) exclude.leaf(idAllocator._end_).
                           value()).longValue();

            String[] strKey = new String[] {Integer.toString(start),
                                            Integer.toString(end)};
            if (cdbExclude.elem(strKey) == null) {
                p.addToReservations(new Range(start, end));
                cdbExclude.create(strKey);
            }
        }

        // then remove those that have been removed
        for(NavuContainer exclude: cdbExclude.elements()) {
            start = (int) ((ConfUInt32) exclude.leaf(idAllocator._start_).
                           value()).longValue();
            end =  (int) ((ConfUInt32) exclude.leaf(idAllocator._end_).
                           value()).longValue();

            String[] strKey = new String[] {Integer.toString(start),
                                            Integer.toString(end)};
            if (poolExclude.elem(strKey) == null) {
                try {
                    p.removeFromReservations(new Range(start, end));
                } catch (Exception e) {
                    LOGGER.error("",e );
                }
                cdbExclude.delete(strKey);
            }
        }

        Pool po = new Pool();
        po.p = p;
        po.reservations = reservations;
        po.availables = availables;
        po.allocations = allocations;
        po.path = pool.getKeyPath();

        pools.add(po);
        System.err.println("done");

    }

    private void safeclose(Cdb s) {
        try {s.close();}
        catch (Exception ignore) {}
    }

    private class Pool {
        IDPool p;
        ReservationsSet reservations;
        AvailablesSet availables;
        AllocationsSet allocations;
        String path;
    }

    private class PoolAlloc {
        Pool p;
        Allocation a;

        public PoolAlloc(Pool p, Allocation a) {
            this.p = p;
            this.a = a;
        }
    }

    private class SyncGroup {
        Pool p;
        String path;

        public SyncGroup(Pool p, String path) {
            this.p = p;
            this.path = path;
        }
    }

    private enum Operation { CREATE, DELETE, SET}
    private enum Type { ALLOC, RANGE, EXCLUDE, POOL }

    private class Request {
        ConfKey pool;
        ConfKey key;
        Operation op;
        Type t;
        ConfPath path;
        ConfValue val;
        int range_start;
        int range_end;
    }

    private class Iter implements CdbDiffIterate {
        CdbSubscription cdbSub;
        Type itype;

        Iter(CdbSubscription sub, Type itype) {
            this.cdbSub = sub;
            this.itype = itype;
        }

        public DiffIterateResultFlag iterate(
            ConfObject[] kp,
            DiffIterateOperFlag op,
            ConfObject oldValue,
            ConfObject newValue, Object initstate) {

            @SuppressWarnings("unchecked")
            ArrayList<Request> reqs = (ArrayList<Request>) initstate;

            try {
                ConfPath p = new ConfPath(kp);
                System.out.println(itype+":ITER " + op + " " + p);

                if (itype == Type.POOL && kp.length > 3)
                    return DiffIterateResultFlag.ITER_RECURSE;

                Request r = new Request();

                r.path = p;
                r.pool = (ConfKey) kp[kp.length-3];
                r.val = null;

                // System.err.println("kp="+kp);
                // System.err.println("kp.length="+kp.length);

                if (kp.length >= 5 && itype != Type.RANGE)
                    r.key = (ConfKey) kp[kp.length-5];
                else
                    r.key = null;

                if (op == DiffIterateOperFlag.MOP_CREATED) {
                    r.op = Operation.CREATE;
                    r.t = itype;
                    reqs.add(r);
                }
                else if (op == DiffIterateOperFlag.MOP_DELETED) {
                    r.op = Operation.DELETE;

                    if (kp.length >= 5)
                        r.t = itype;
                    else
                        r.t = Type.POOL;

                    if (r.t == Type.ALLOC) {
                        ConfValue v =
                            wsess.getElem(r.path+"/"+
                                          resourceAllocator.
                                          _response_+"/"
                                          +resourceAllocator._id_);
                        r.val = v;
                    }

                    reqs.add(r);
                }
                else if (op == DiffIterateOperFlag.MOP_VALUE_SET &&
                         itype == Type.RANGE) {
                    // range modified
                    r.op = Operation.SET;
                    r.t = itype;
                    r.range_start =
                        (int) ((ConfUInt32) isess.getElem(r.path+"/../"+
                                                          idAllocator._start_)).
                        longValue();;
                    r.range_end =
                        (int) ((ConfUInt32) isess.getElem(r.path+"/../"+
                                                          idAllocator._end_)).
                        longValue();;

                    System.err.println("range_start: "+r.range_start);
                    System.err.println("range_end: "+r.range_end);

                    boolean foundRange=false;
                    for(Request req: reqs) {
                        if (req.t == Type.RANGE && req.pool == r.pool) {
                            // we only need one RANGE set since it
                            // will process both start and end changes
                            foundRange = true;
                        }
                    }

                    if (foundRange == false)
                        reqs.add(r);
                }
                else {
                    // ignore VALUE_SET etc
                }
            }
            catch (Exception e) {
                LOGGER.error("", e);
            }
            return DiffIterateResultFlag.ITER_RECURSE;
        }
    }

    // redeploy MUST be done in another thread, if not system
    // hangs, since the CDB subscriber cannot do its work
    private void redeploy(String path) {
        Redeployer r = new Redeployer(path);
        Thread t = new Thread(r);
        t.start();
    }

    private class Redeployer implements Runnable {
        private String path;
        private ConfKey k;
        private Maapi m;
        private Socket s;

        public Redeployer(String path) {
            this.path = path; this.k = k;

            try {
                s = new Socket(NcsMain.getInstance().getNcsHost(),
                               NcsMain.getInstance().getNcsPort());
                m = new Maapi(s);

                m.startUserSession("admin",
                                   m.getSocket().getInetAddress(),
                                   "system",
                                   new String[] {"admin"},
                                   MaapiUserSessionFlag.PROTO_TCP);
            } catch (Exception e) {
                System.err.println("redeployer exception: "+e);
            }

        }

        public void run() {
            try {
                // must be different, we want to redeploy owner if
                // he exists
                int tid = m.startTrans(Conf.DB_RUNNING, Conf.MODE_READ);
                System.err.println("invoking redeploy on "+path);

                int counter = 0;
                while (true) {
                  Thread.sleep(50);
                  if (m.exists(tid, path))
                    break;
                  if (counter++ == 40) {
                    break;
                  }
                  Thread.sleep(1000);
                }


                m.requestAction(new ConfXMLParam[] {},
                                path+"/reactive-re-deploy");
                try {
                    m.finishTrans(tid);
                }
                catch (Throwable ignore) {
                }
                s.close();
            } catch (Exception e) {
                LOGGER.error("error in reactive-re-deploy: "+path, e);
                return;
            }
        }
    }
}
