/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class AllocationException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public AllocationException() { super(); }
        public AllocationException(String msg) { super(msg); }
        public AllocationException(Exception e) { super(e); }
        public AllocationException(String msg, Exception e) { super(msg, e); }
}
