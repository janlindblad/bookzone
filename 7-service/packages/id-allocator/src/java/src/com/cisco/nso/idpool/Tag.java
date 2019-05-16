package com.cisco.nso.idpool;

import java.io.Serializable;

public class Tag implements Serializable {
        private static final long serialVersionUID = -7671139904147340643L;

        /*
         * HACK
         */
        private String uuid;
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        private String name;
        private String scope;

        public Tag() {
        }

        public Tag(String name, String scope) {
                this.name = name;
                this.scope = scope;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
                return "{\"name\":" + name + ",\"scope\":" + scope + "}";
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
                final int prime = 31;
                int result = 1;

                if (name != null)
                        result = prime * result + name.hashCode();
                if (scope != null)
                        result = prime * result + scope.hashCode();
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
                Tag other = (Tag) obj;
                if (name != other.name)
                        return false;
                if (scope != other.scope)
                        return false;
                return true;
        }
}
