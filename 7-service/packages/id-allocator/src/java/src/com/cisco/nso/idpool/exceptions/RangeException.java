/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class RangeException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public RangeException() { super(); }
        public RangeException(String msg) { super(msg); }
        public RangeException(Exception e) { super(e); }
        public RangeException(String msg, Exception e) { super(msg, e); }
}
