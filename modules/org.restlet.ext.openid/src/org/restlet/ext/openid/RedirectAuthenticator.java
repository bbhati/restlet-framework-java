/**
 * Copyright 2005-2011 Noelios Technologies.
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

package org.restlet.ext.openid;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Status;
import org.restlet.security.Authenticator;
import org.restlet.security.User;
import org.restlet.security.Verifier;

/**
 * An Authenticator that redirects the authentication to some external resource.
 * After successfull authentication, the Authenticator will do a redirect to the
 * original request resourceRef. The RedirectAuthenticator keeps track of state
 * using a session cookie which is not automatically cleaned.
 * 
 * The typical use case for this {@link Authenticator} is to do remote
 * authentication using OpenId.
 * 
 * The RedirectAuthenticator have the following logic base on {@link Verifier}
 * returns:
 * <ol>
 * <li>If the verifier returns {@link Verifier#RESULT_VALID} it will clean up
 * any unneeded cookies and do a
 * {@link Response#redirectPermanent(org.restlet.data.Reference)} to the
 * original resource
 * <li>If the result is {@link Verifier#RESULT_INVALID} or
 * {@link Verifier#RESULT_UNKNOWN} it will clean upp all cookies and call forbid
 * (default behavior to set {@link Status#CLIENT_ERROR_FORBIDDEN} if no
 * errorResource has been set)
 * <li>If the result is any other it will clean up the identifierCookie.
 * </ol>
 * 
 * <pre>
 * 
 * </pre>
 * 
 * @author Martin Svensson
 * 
 */
public class RedirectAuthenticator extends Authenticator {
    /** The default name of the cookie that contains the identifier. */
    public final static String DefaultIdentifierCookie = "session_id";

    /**
     * The default name of the cookie that contains the original request's
     * reference.
     */
    public final static String DefaultOrigRefCookie = "original_ref";

    public final static String OriginalRefAttribute = "origRef";

    /** The verifier of the credentials. */
    private final Verifier verifier;

    /** The current name of the cookie that contains the identifier. */
    private final String identifierCookie;

    /**
     * The current name of the cookie that contains the original request's
     * reference.
     */
    private final String origRefCookie;

    // private final String errorResource;
    /**
     * The restlet in charge of handling authentication or authorization
     * failure.
     */
    private Restlet forbiddenResource;

    /**
     * Initialize a RedirectAuthenticator with a Verifier.
     * 
     * @param context
     *            - Context
     * @param verifier
     *            - A Verifier that sets user identifier upon completion
     */
    public RedirectAuthenticator(Context context, Verifier verifier,
            Restlet forbiddenResource) {
        super(context);
        this.forbiddenResource = forbiddenResource;
        this.verifier = verifier;
        this.origRefCookie = DefaultOrigRefCookie;
        this.identifierCookie = DefaultIdentifierCookie;
    }

    /**
     * Initializes a RedirectAuthenticator with a Verifier.
     * 
     * @param context
     *            The context.
     * @param verifier
     *            The verifier that sets user identifier upon completion.
     * @param identifierCookie
     *            The name of the cookie that contains the identifier.
     * @param origRefCookie
     *            The name of the cookie that contains the original request's
     *            reference.
     * @param forbiddenResource
     *            The Restlet that will handle the call in case of
     *            authentication or authorization failure.
     */
    public RedirectAuthenticator(Context context, Verifier verifier,
            String identifierCookie, String origRefCookie,
            Restlet forbiddenResource) {
        super(context);
        this.forbiddenResource = forbiddenResource;
        this.verifier = verifier;
        this.identifierCookie = identifierCookie != null ? identifierCookie
                : DefaultIdentifierCookie;
        this.origRefCookie = origRefCookie != null ? origRefCookie
                : DefaultOrigRefCookie;
    }

    @Override
    protected boolean authenticate(Request request, Response response) {
        // Form f = request.getResourceRef().getQueryAsForm();
        User u = request.getClientInfo().getUser();
        String identifier = request.getCookies()
                .getFirstValue(identifierCookie);
        String origRef;
        if (identifier != null) {
            u = new User(identifier);
            request.getClientInfo().setUser(u);
            return true;
        }
        if (request.getCookies().getFirstValue(origRefCookie) == null) {
            origRef = request.getResourceRef().toString();
            response.getCookieSettings().add(origRefCookie,
                    request.getResourceRef().toString());
        } else {
            origRef = request.getCookies().getFirstValue(origRefCookie);
        }

        int verified = verifier.verify(request, response);
        getLogger().info("VERIFIED: " + verified);

        if (verified == Verifier.RESULT_VALID) {
            response.getCookieSettings().removeAll(identifierCookie);
            response.getCookieSettings().add(identifierCookie,
                    request.getClientInfo().getUser().getIdentifier());
            handleUser(request.getClientInfo().getUser());
            // String origRef =
            // request.getCookies().getFirstValue(origRefCookie);
            request.getCookies().removeAll(origRefCookie);
            response.getCookieSettings().removeAll(origRefCookie);
            if (origRef != null) {
                response.redirectPermanent(origRef);

            }
            return true;
        }
        response.getCookieSettings().removeAll(identifierCookie);
        if (verified == Verifier.RESULT_UNKNOWN
                || verified == Verifier.RESULT_INVALID) {

            origRef = response.getCookieSettings().getFirstValue(origRefCookie);
            if (origRef == null)
                origRef = request.getCookies().getFirstValue(origRefCookie);
            // request.getCookies().removeAll(origRefCookie);
            // response.getCookieSettings().removeAll(origRefCookie);
            forbid(origRef, request, response);
        }

        return false;
    }

    /**
     * Handles the retrieved user from the verifier. The only thing that will be
     * stored is the user identifier (in a cookie). Should be overriden as it
     * does nothing by default.
     * 
     * @param user
     *            The user.
     */
    protected void handleUser(User user) {
        ;
    }

    /**
     * Rejects the call due to a failed authentication or authorization. This
     * can be overridden to change the default behavior, for example to display
     * an error page. By default, forbid does a Server Redirect to the
     * errorResource (if provided) otherwise it will set the response status to
     * ClIENT_ERROR_FORBIDDEN
     * 
     * @param origRef
     *            The original ref stored by the RedirectAuthenticator
     * @param request
     *            The rejected request.
     * @param response
     *            The reject response.
     */
    public void forbid(String origRef, Request request, Response response) {
        if (forbiddenResource == null)
            response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
        else {
            getLogger().info("sending to error resource");
            forbiddenResource.handle(request, response);
            // Reference ref = new Reference(errorResource);
            // if(origRef != null)
            // ref.addQueryParameter("origRef", origRef);
            // Redirector r = new Redirector(getContext(), ref.toString(),
            // Redirector.MODE_SERVER_OUTBOUND);
            // r.handle(request, response);
        }

    }

    @Override
    protected int unauthenticated(Request request, Response response) {
        int ret = super.unauthenticated(request, response);
        getLogger().info("UNAUTHENTICATED REQUEST!!!");
        return ret;
    }

}