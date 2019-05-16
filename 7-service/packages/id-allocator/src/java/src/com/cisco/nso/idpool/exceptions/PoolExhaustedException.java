/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class PoolExhaustedException extends AllocationException {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public PoolExhaustedException() { super(); }
        public PoolExhaustedException(String msg) { super(msg); }
        public PoolExhaustedException(Exception e) { super(e); }
        public PoolExhaustedException(String msg, Exception e) {
          super(msg, e);
        }
}
