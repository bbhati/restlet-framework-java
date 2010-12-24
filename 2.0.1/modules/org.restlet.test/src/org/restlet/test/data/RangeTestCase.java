/**
 * Copyright 2005-2010 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.test.data;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.restlet.Application;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.LocalReference;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Range;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.engine.io.BioUtils;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.test.RestletTestCase;

/**
 * Test {@link org.restlet.data.Range}.
 * 
 * @author Jerome Louvel
 */
public class RangeTestCase extends RestletTestCase {

    /**
     * Internal class used for test purpose.
     * 
     */
    private static class TestRangeApplication extends Application {

        public TestRangeApplication() {
            super();
            getRangeService().setEnabled(true);
        }

        @Override
        public Restlet createInboundRoot() {
            Router router = new Router();
            router.attach("/test", new TestRangeRestlet());
            router.attach("/testGet", new TestRangeGetRestlet());
            Directory directory = new Directory(getContext(),
                    LocalReference.createFileReference(testDir));
            directory.setModifiable(true);
            router.attach("/testPut/", directory);
            return router;
        }
    }

    /**
     * Internal class used for test purpose. It simply returns a string 10
     * characters long.
     * 
     */
    private static class TestRangeGetRestlet extends Restlet {
        @Override
        public void handle(Request request, Response response) {
            response.setEntity(new StringRepresentation("1234567890"));
            response.getEntity().setTag(new Tag("TestRangeGetRestlet"));
        }
    }

    /**
     * Internal class used for test purpose. It tests the list of ranges sent by
     * the request and compares it with the values sent into the query.
     * 
     */
    private static class TestRangeRestlet extends Restlet {

        @Override
        public void handle(Request request, Response response) {
            Form form = request.getResourceRef().getQueryAsForm();
            List<Range> ranges = request.getRanges();

            boolean match = false;
            for (Parameter parameter : form) {
                long index = 0;
                long length = 0;
                String value = parameter.getValue();
                if (value.startsWith("-")) {
                    index = Range.INDEX_LAST;
                    length = Long.parseLong(value.substring(1));
                } else if (value.endsWith("-")) {
                    index = Long.parseLong(value.substring(0,
                            value.length() - 1));
                    length = Range.SIZE_MAX;
                } else {
                    String[] tab = value.split("-");
                    if (tab.length == 2) {
                        index = Long.parseLong(tab[0]);
                        length = Long.parseLong(tab[1]) - index;
                    }
                }

                boolean found = false;
                for (Range range : ranges) {
                    found = (index == range.getIndex())
                            && (length == range.getSize());
                    if (found) {
                        break;
                    }
                }
                if (!found) {
                    break;
                }
                match = true;
            }
            if (match) {
                response.setStatus(Status.SUCCESS_OK);
            } else {
                response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }
    }

    // Create a temporary directory for the tests
    private static final File testDir = new File(
            System.getProperty("java.io.tmpdir"), "rangeTestCase");

    /** Component used for the tests. */
    private Component component;

    @Override
    protected void setUp() throws Exception {
        component = new Component();
        component.getServers().add(Protocol.HTTP, TEST_PORT);
        component.getClients().add(Protocol.FILE);
        component.getDefaultHost().attach(new TestRangeApplication());
        component.start();
    }

    @Override
    protected void tearDown() throws Exception {
        component.stop();
        component = null;
    }

    /**
     * Tests partial Get requests.
     * 
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {
        Client client = new Client(Protocol.HTTP);
        // Test partial Get.
        Request request = new Request(Method.GET, "http://localhost:"
                + TEST_PORT + "/testGet");
        Response response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("1234567890", response.getEntity().getText());
        assertEquals(10, response.getEntity().getSize());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/testGet");
        request.setRanges(Arrays.asList(new Range(0, 10)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("1234567890", response.getEntity().getText());
        assertEquals(10, response.getEntity().getSize());
        assertEquals(0, response.getEntity().getRange().getIndex());
        assertEquals(10, response.getEntity().getRange().getSize());

        request.setRanges(Arrays.asList(new Range(Range.INDEX_FIRST, 2)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("12", response.getEntity().getText());
        assertEquals(2, response.getEntity().getSize());
        assertEquals(0, response.getEntity().getRange().getIndex());
        assertEquals(2, response.getEntity().getRange().getSize());

        request.setRanges(Arrays.asList(new Range(2, 2)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("34", response.getEntity().getText());
        assertEquals(2, response.getEntity().getSize());
        assertEquals(2, response.getEntity().getRange().getIndex());
        assertEquals(2, response.getEntity().getRange().getSize());

        request.setRanges(Arrays.asList(new Range(2, 7)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("3456789", response.getEntity().getText());
        assertEquals(7, response.getEntity().getSize());
        assertEquals(2, response.getEntity().getRange().getIndex());
        assertEquals(7, response.getEntity().getRange().getSize());

        request.setRanges(Arrays.asList(new Range(Range.INDEX_LAST, 7)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("4567890", response.getEntity().getText());
        assertEquals(7, response.getEntity().getSize());
        assertEquals(3, response.getEntity().getRange().getIndex());
        assertEquals(7, response.getEntity().getRange().getSize());

        request.setRanges(Arrays.asList(new Range(1, Range.SIZE_MAX)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("234567890", response.getEntity().getText());
        assertEquals(9, response.getEntity().getSize());
        assertEquals(1, response.getEntity().getRange().getIndex());
        assertEquals(9, response.getEntity().getRange().getSize());

        client.stop();
    }

    /**
     * Tests conditional ranges requests.
     * 
     * @throws Exception
     */
    @Test
    public void testConditionalRanges() throws Exception {
        Client client = new Client(Protocol.HTTP);
        // Test partial Get.
        Request request = new Request(Method.GET, "http://localhost:"
                + TEST_PORT + "/testGet");
        Response response = client.handle(request);
        Tag entityTag = response.getEntity().getTag();

        request.setRanges(Arrays.asList(new Range(1, Range.SIZE_MAX)));
        request.getConditions().setRangeTag(entityTag);
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("234567890", response.getEntity().getText());
        assertEquals(9, response.getEntity().getSize());
        assertEquals(1, response.getEntity().getRange().getIndex());
        assertEquals(9, response.getEntity().getRange().getSize());

        entityTag = new Tag(entityTag.getName() + "-test");
        request.setRanges(Arrays.asList(new Range(1, Range.SIZE_MAX)));
        request.getConditions().setRangeTag(entityTag);
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("1234567890", response.getEntity().getText());
        client.stop();
    }

    /**
     * Tests partial Put requests.
     * 
     * @throws IOException
     */
    @Test
    public void testPut() throws IOException {
        BioUtils.delete(testDir, true);
        Client client = new Client(Protocol.HTTP);

        // PUT on a file that does not exist
        Request request = new Request(Method.PUT, "http://localhost:"
                + TEST_PORT + "/testPut/essai.txt");
        request.setEntity(new StringRepresentation("1234567890"));
        request.setRanges(Arrays.asList(new Range(0, 10)));
        Response response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        response = client.handle(new Request(Method.GET, request
                .getResourceRef()));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("1234567890", response.getEntity().getText());

        // Partial PUT on a file, the provided representation overflowed the
        // existing file
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai.txt");
        request.setEntity(new StringRepresentation("0000000000"));
        request.setRanges(Arrays.asList(new Range(1, 10)));
        response = client.handle(request);
        System.out.println(response.getStatus() + " / "
                + response.getStatus().getThrowable());
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        response = client.handle(new Request(Method.GET, request
                .getResourceRef()));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("10000000000", response.getEntity().getText());

        // Partial PUT on a file that does not exists, the provided range
        // does not start at the 0 index.
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai2.txt");
        request.setEntity(new StringRepresentation("0000000000"));
        request.setRanges(Arrays.asList(new Range(1, 10)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        request.setMethod(Method.GET);
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("0000000000", response.getEntity().getText());

        // Partial PUT on a file, simple range
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai.txt");
        request.setEntity(new StringRepresentation("22"));
        request.setRanges(Arrays.asList(new Range(2, 2)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        response = client.handle(new Request(Method.GET, request
                .getResourceRef()));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("10220000000", response.getEntity().getText());

        // Partial PUT on a file, the provided representation will be padded
        // at the very end of the file.
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai.txt");
        request.setEntity(new StringRepresentation("888"));
        request.setRanges(Arrays.asList(new Range(8, Range.SIZE_MAX)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        response = client.handle(new Request(Method.GET, request
                .getResourceRef()));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("10220000888", response.getEntity().getText());

        // Partial PUT on a file that does not exist, the range does not
        // specify the range size.
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai3.txt");
        request.setEntity(new StringRepresentation("888"));
        request.setRanges(Arrays.asList(new Range(8, Range.SIZE_MAX)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        request.setMethod(Method.GET);
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("888", response.getEntity().getText());

        // Partial PUT on a file, the provided representation will be padded
        // just before the end of the file.
        request = new Request(Method.PUT, "http://localhost:" + TEST_PORT
                + "/testPut/essai.txt");
        request.setEntity(new StringRepresentation("99"));
        request.setRanges(Arrays.asList(new Range(8, Range.SIZE_MAX)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        response = client.handle(new Request(Method.GET, request
                .getResourceRef()));
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("10220000998", response.getEntity().getText());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/testPut/essai.txt");
        request.setRanges(Arrays.asList(new Range(3, Range.SIZE_MAX)));
        response = client.handle(request);
        assertEquals(Status.SUCCESS_PARTIAL_CONTENT, response.getStatus());
        assertEquals("20000998", response.getEntity().getText());

        BioUtils.delete(testDir, true);
    }

    /**
     * Tests ranges.
     * 
     * @throws Exception
     */
    @Test
    public void testRanges() throws Exception {
        Client client = new Client(Protocol.HTTP);

        // Test "range" header.
        Request request = new Request(Method.GET, "http://localhost:"
                + TEST_PORT + "/test?range=0-500");
        request.setRanges(Arrays.asList(new Range(0, 500)));
        assertEquals(Status.SUCCESS_OK, client.handle(request).getStatus());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/test?range=-500");
        request.setRanges(Arrays.asList(new Range(Range.INDEX_LAST, 500)));
        assertEquals(Status.SUCCESS_OK, client.handle(request).getStatus());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/test?range=500-");
        request.setRanges(Arrays.asList(new Range(500, Range.SIZE_MAX)));
        assertEquals(Status.SUCCESS_OK, client.handle(request).getStatus());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/test?range=500-1000");
        request.setRanges(Arrays.asList(new Range(500, 500)));
        assertEquals(Status.SUCCESS_OK, client.handle(request).getStatus());

        request = new Request(Method.GET, "http://localhost:" + TEST_PORT
                + "/test?range=500-1000&range=500-");
        request.setRanges(Arrays.asList(new Range(500, 500), new Range(500,
                Range.SIZE_MAX)));
        assertEquals(Status.SUCCESS_OK, client.handle(request).getStatus());

        client.stop();
    }
}