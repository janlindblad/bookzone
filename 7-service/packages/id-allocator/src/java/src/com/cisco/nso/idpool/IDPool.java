package com.cisco.nso.idpool;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.cisco.nso.idpool.exceptions.AllocationException;
import com.cisco.nso.idpool.exceptions.DuplicateAllocationException;
import com.cisco.nso.idpool.exceptions.InvalidRangeException;
import com.cisco.nso.idpool.exceptions.OutOfRangeException;
import com.cisco.nso.idpool.exceptions.PoolExhaustedException;

public class IDPool {

    private String name;

    private Set<Range> reservations;
    private Set<Range> availables;
    private Set<Allocation> allocations;

    private Range poolRange = new Range(0,1);

    // private static final Range RESERVED1 = new Range(3968, 4048);
    // private static final Range RESERVED2 = new Range(4094, 4094);
    // private static final Range RESERVED3 = new Range(1002, 1005);

    private Set<Tag> tags;

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        if (tags == null)
            tags = new HashSet<Tag>();
        tags.add(tag);
    }

    public IDPool(String name,
                  Set<Range> reservations,
                  Set<Range> availables,
                  Set<Allocation> allocations) {
        this.name = name;
        this.reservations = reservations;
        this.availables = availables;
        this.allocations = allocations;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Set<Range> getReservations() {
        return reservations;
    }

    public synchronized void addToReservations(Range range) {
        reservations.add(range);
        recalculateRanges();
    }

    public synchronized void removeFromReservations(Range range) {
        reservations.remove(range);
        recalculateRanges();
    }

    public Set<Range> getAvailables() {
        return availables;
    }

    public Set<Allocation> getAllocations() {
        return allocations;
    }

    public synchronized void addAllocation(Allocation a) {
        this.allocations.add(a);
    }

    public synchronized void clearAllocation() {
        this.allocations.clear();
    }

    public Range getPoolRange() {
        return poolRange;
    }

    public synchronized Allocation allocate(String occupant)
        throws AllocationException {
        if (availables.isEmpty()) {
            throw new PoolExhaustedException("ID pool exhausted");
        }

        Range range = availables.iterator().next();
        int result = range.getStart();

        availables.remove(range);
        range = new Range(result+1,range.getEnd());

        if (range.getStart() <= range.getEnd()) {
            // Range is not exhausted...add it to availables
            availables.add(range);
        }

        Allocation allocation = new Allocation(result, occupant);
        allocations.add(allocation);
        return allocation;
    }

    public synchronized Allocation allocate(String occupant, int requested)
        throws AllocationException {
        if (availables.isEmpty()) {
            throw new PoolExhaustedException("ID pool exhausted");
        }

        for(Range range: availables) {
            if (range.getStart() <= requested && range.getEnd() >= requested) {
                availables.remove(range);

                Range before = new Range(range.getStart(), requested-1);
                Range after = new Range(requested+1, range.getEnd());

                if (before.getStart() <= before.getEnd())
                    availables.add(before);
                if (after.getStart() <= after.getEnd())
                    availables.add(after);

                Allocation allocation = new Allocation(requested, occupant);
                allocations.add(allocation);
                return allocation;
            }
        }

        throw new PoolExhaustedException("Requested id not available");
    }

    public synchronized void setRange(Range range)
        throws InvalidRangeException {

        int start = range.getStart();
        int end = range.getEnd();

        for (Allocation id : allocations) {
            if (!(id.getAllocated() >= start && id.getAllocated() <= end)) {
                throw new InvalidRangeException("Specified range is invalid.");
            }
        }

        poolRange = new Range(start,end);

        // System.err.println("Before recalculateRanges");
        // printAvailables();

        recalculateRanges();

        // System.err.println("After recalculateRanges");
        // printAvailables();
    }

    private void printAvailables() {
        for (Range range: availables)
            System.err.println(range);
    }


    private void recalculateRanges() {
        HashSet<Range> tmp = new HashSet<Range>();

        tmp.add(poolRange);
        excludeReservedRange(tmp);

        for (Allocation id : allocations) {
            Set<Range> oldAvailables = new HashSet<Range>(tmp);
            for (Range r : oldAvailables) {
                if (id.getAllocated() >= r.getStart() &&
                    id.getAllocated() <= r.getEnd()) {
                    tmp.remove(r);
                    if (id.getAllocated() == r.getStart()) {
                        if (id.getAllocated() < r.getEnd())
                            tmp.add(new Range(id.getAllocated()+1,
                                              r.getEnd()));
                    } else if (id.getAllocated() == r.getEnd()) {
                        // we know id.getAllocated() > r.getStart()
                        tmp.add(new Range(r.getStart(),
                                          id.getAllocated()-1));
                    } else {
                        // need to split the range
                        tmp.add(new Range(id.getAllocated()+1,
                                          r.getEnd()));
                        tmp.add(new Range(r.getStart(),
                                          id.getAllocated()-1));
                    }
                    break;
                }
            }
        }

        /* first remove all that are the same from tmp, and
         * removed from availables
         */
        HashSet<Range> toRemove = new HashSet<Range>();
        for (Range r: availables) {
            if (tmp.contains(r)) {
                // System.err.println("found existing availables: "+r);
                tmp.remove(r);
            }
            else {
                // System.err.println("no longer available: "+r);
                toRemove.add(r);
            }
        }

        for (Range r: toRemove) {
            availables.remove(r);
        }

        /* then add all new */
        for (Range r: tmp) {
            // System.err.println("new avaialble: "+r);
            availables.add(r);
        }
    }

    /**
     * Excludes reserved id range from the list of available ranges
     */
    private void excludeReservedRange(HashSet<Range> tmp) {
        for (Range r : reservations) {
            Set<Range> oldAvailables = new HashSet<Range>(tmp);
            for (Range range : oldAvailables) {
                if ((range.getStart() < r.getStart() &&
                     range.getEnd() < r.getStart())
                    ||
                    (range.getStart() > r.getEnd() &&
                     range.getEnd() > r.getEnd()))
                    continue;

                if (range.getStart() <= r.getStart()) {
                    tmp.remove(range);
                    tmp.add(new Range(range.getStart(),
                                             r.getStart()-1));
                    if (range.getEnd() > r.getEnd()) {
                        tmp.add(new Range(r.getEnd() + 1,
                                                 range.getEnd()));
                    }

                    break;
                } else if (range.getStart() > r.getStart()) {
                    tmp.remove(range);
                    if (range.getEnd() > r.getEnd()) {
                        tmp.add(new Range(r.getEnd()+1,
                                                 range.getEnd()));
                    }

                    break;
                }
            }
        }
    }

    private void compact(Range range) {
        // printAvailables();

        Set<Range> oldAvailables = new HashSet<Range>(availables);
        for (Range arange: oldAvailables) {
            if (arange.equals(range))
                // skip myself
                continue;

            if (arange.getStart() == range.getEnd()+1) {
                availables.remove(range);
                availables.remove(arange);
                Range merged = new Range(range.getStart(),
                                         arange.getEnd());
                availables.add(merged);
                compact(merged);
                printAvailables();
                return;
            }
            else if (arange.getEnd() == range.getStart()-1) {
                availables.remove(range);
                availables.remove(arange);
                Range merged = new Range(arange.getStart(),
                                         range.getEnd());
                availables.add(merged);
                printAvailables();
                return;
            }
        }
        printAvailables();
    }

    public synchronized void release(int id)
        throws AllocationException {

        for(Allocation alloc: allocations) {
            if (alloc.getAllocated().equals(id)) {
                release(alloc);
                return;
            }
        }
    }

    public synchronized void release(Allocation allocation)
        throws AllocationException {
        if (!allocations.contains(allocation)) {
            throw new AllocationException("allocation " +
                                          allocation +
                                          " is not allocated from the pool");
        }

        allocations.remove(allocation);
        int id = allocation.getAllocated();

        Set<Range> oldAvailables = new HashSet<Range>(availables);
        for (Range range : oldAvailables) {
            if (range.getStart() == id+1) {
                availables.remove(range);
                Range newr = new Range(id, range.getEnd());
                availables.add(newr);
                compact(newr);
                return;
            }
            else if (range.getEnd() == id-1) {
                availables.remove(range);
                Range newr = new Range(range.getStart(),id);
                availables.add(newr);
                compact(newr);
                return;
            }
        }
        availables.add(new Range(id, id));
    }

    public boolean isAvailable(int id) {
        for (Range range : availables) {
            if (id >= range.getStart() && id <= range.getEnd()) return true;
        }
        return false;
    }
}
