/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class OutOfRangeException extends AllocationException {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public OutOfRangeException() { super(); }
        public OutOfRangeException(String msg) { super(msg); }
        public OutOfRangeException(Exception e) { super(e); }
        public OutOfRangeException(String msg, Exception e) { super(msg, e); }
}
