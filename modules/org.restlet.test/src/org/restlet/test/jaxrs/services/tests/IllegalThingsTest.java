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
package org.restlet.test.jaxrs.services.tests;

import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.test.jaxrs.services.resources.IllegalThingsTestService;

/**
 * Checks, if illegal things are forbidden.
 * 
 * @author Stephan Koops
 * @see IllegalThingsTestService
 */
public class IllegalThingsTest extends JaxRsTestCase {

    @Override
    protected Class<?> getRootResourceClass() {
        return IllegalThingsTestService.class;
    }

    public void testNullSubResource() throws Exception {
        final Response response = get("nullSubResource");
        assertEquals(Status.SERVER_ERROR_INTERNAL, response.getStatus());
    }

    public void testPackage() throws Exception {
        final Response response = get("package");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }

    public void testPrivate() throws Exception {
        final Response response = get("private");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }

    public void testProtected() throws Exception {
        final Response response = get("protected");
        assertEquals(Status.CLIENT_ERROR_NOT_FOUND, response.getStatus());
    }
}