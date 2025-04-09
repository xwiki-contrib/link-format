/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.linkformat.macros;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;

/**
 * Parameters for link-group macro.
 *
 * @version $Id: $
 * @since 1.1.0
 */
public class LinkGroupParameters
{
    private org.xwiki.model.reference.DocumentReference reference;

    /**
     * Get the reference set in the link.
     *
     * @return the link to point
     */
    public DocumentReference getReference()
    {
        return reference;
    }

    /**
     * Set the reference set in the link.
     *
     * @param reference the link to point
     */
    @PropertyDescription("The link to point with the clickable macro. Can be a document reference or a http link")
    @PropertyMandatory
    public void setReference(DocumentReference reference)
    {
        this.reference = reference;
    }
}
