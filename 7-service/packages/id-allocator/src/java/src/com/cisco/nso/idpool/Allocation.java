package com.cisco.nso.idpool;

import java.io.Serializable;

public class Allocation implements Serializable {
    private Integer allocated;
    private String occupant;

    public Allocation() {
    }

    public Allocation(int allocated, String occupant) {
        this.allocated = allocated;
        this.occupant = occupant;
    }

    public Allocation(Allocation that) {
        super();
        this.allocated = that.allocated;
        this.occupant = that.occupant;
    }

    public Integer getAllocated() { return allocated; }
    public void setAllocated(Integer segment) { this.allocated = segment; }

    public String getOccupant() { return occupant; }
    public void setOccupant(String occupant) { this.occupant = occupant; }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "{\"segment\":" + allocated + ",\"occupant\":" + occupant + "}";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + allocated;
        result = prime * result + occupant.hashCode();
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Allocation other = (Allocation) obj;
        if (allocated != other.allocated)
            return false;
        if (occupant != other.occupant)
            return false;
        return true;
    }
}
