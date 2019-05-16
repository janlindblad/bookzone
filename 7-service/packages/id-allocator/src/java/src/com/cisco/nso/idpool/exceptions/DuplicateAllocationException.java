/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class DuplicateAllocationException extends AllocationException {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public DuplicateAllocationException() { super(); }
        public DuplicateAllocationException(String msg) { super(msg); }
        public DuplicateAllocationException(Exception e) { super(e); }
        public DuplicateAllocationException(String msg, Exception e) {
          super(msg, e);
        }
}
