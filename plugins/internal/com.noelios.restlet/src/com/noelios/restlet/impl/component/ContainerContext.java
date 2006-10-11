/*
 * Copyright 2005-2006 Noelios Consulting.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * http://www.opensource.org/licenses/cddl1.txt
 * If applicable, add the following below this CDDL
 * HEADER, with the fields enclosed by brackets "[]"
 * replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.impl.component;

import java.util.logging.Logger;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;

/**
 * Context allowing access to the container's connectors.
 * @author Jerome Louvel (contact@noelios.com) <a href="http://www.noelios.com/">Noelios Consulting</a>
 */
public class ContainerContext extends ContextImpl
{
	/** The parent container. */
	private ContainerImpl container;

	/**
	 * Constructor. 
	 * @param container The parent container.
    * @param logger The logger instance of use.
	 */
	public ContainerContext(ContainerImpl container, Logger logger)
	{
		super(logger);
		this.container = container;
	}
	
	/**
    * Handles a call.
    * @param protocol The protocol to use for the handling.
    * @param request The request to handle.
    * @param response The response to update.
    */
   public void handle(Protocol protocol, Request request, Response response)
   {
		getContainer().getClientRouter().handle(request, response);
   }

	/**
	 * Returns the parent container.
	 * @return The parent container.
	 */
	protected ContainerImpl getContainer()
	{
		return this.container;
	}

	/**
	 * Sets the parent container.
	 * @param container The parent container.
	 */
	protected void setContainer(ContainerImpl container)
	{
		this.container = container;
	}
}
