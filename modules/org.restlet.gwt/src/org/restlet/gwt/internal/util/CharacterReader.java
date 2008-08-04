/*
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). 
 * You can select the license that you prefer but you may not use this file 
 * except in compliance with one of these Licenses.
 *
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and 
 * limitations under the Licenses. 
 *
 * Alternatively, you can obtain a royaltee free commercial license with 
 * less limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine/.
 *
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.gwt.internal.util;

/**
 * Character reader.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public class CharacterReader {

    /** The text to read. */
    private final String text;

    /** The next position to read. */
    private int position;

    /**
     * Constructor.
     * 
     * @param text
     *            The source text to read.
     */
    public CharacterReader(String text) {
        this.text = text;
        this.position = 0;
    }

    /**
     * Reads the next character in the source text.
     * 
     * @return The next character or -1 if end of text is reached.
     */
    public int read() {
        return (this.position == this.text.length()) ? -1 : this.text
                .charAt(this.position++);
    }

}
