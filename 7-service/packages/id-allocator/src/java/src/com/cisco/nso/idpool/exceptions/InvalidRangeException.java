/**
 *
 */
package com.cisco.nso.idpool.exceptions;

/**
 * @author amikumar
 *
 */
public class InvalidRangeException extends RangeException {
        /**
         *
         */
        private static final long serialVersionUID = -8873212152590915617L;

        public InvalidRangeException() { super(); }
        public InvalidRangeException(String msg) { super(msg); }
        public InvalidRangeException(Exception e) { super(e); }
        public InvalidRangeException(String msg, Exception e) { super(msg, e); }
}
