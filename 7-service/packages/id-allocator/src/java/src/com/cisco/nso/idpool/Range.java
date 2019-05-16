package com.cisco.nso.idpool;

import java.io.Serializable;

public class Range implements Serializable {
    private int start;
    private int end;

    public Range() {
    }

    public Range(int start, int end) {
        super();
        this.start = start;
        this.end = end;
    }

    public Range(Range that) {
        super();
        this.start = that.start;
        this.end = that.end;
    }

    public int getStart() {
        return start;
    }

    // public void setStart(int start) {
    //     this.start = start;
    // }

    public int getEnd() {
        return end;
    }

    // public void setEnd(int end) {
    //     this.end = end;
    // }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "{\"start\":" + start + ",\"end\":" + end + "}";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + start;
        result = prime * result + end;
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
        Range other = (Range) obj;
        if (start != other.start)
            return false;
        if (end != other.end)
            return false;
        return true;
    }

    public boolean contains(int vlanNo) {
        return vlanNo >= start && vlanNo <= end;
    }
}
