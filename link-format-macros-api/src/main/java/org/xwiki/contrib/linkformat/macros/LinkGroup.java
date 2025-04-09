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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.url.URLSecurityManager;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * link-group macro.
 *
 * @version $Id: $
 * @since 1.1.0
 */
@Component
@Named("link-group")
@Singleton
public class LinkGroup extends AbstractMacro<LinkGroupParameters>
{
    private static final String NAME = "Link group";

    private static final String DESCRIPTION =
        "The link to point with the clickable macro. Can be a document reference or a http link.";

    private static final String CONTENT_DESCRIPTION = "The content to be displayed in the link.";

    private static final String END_LINK = "</a>";

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private MacroContentParser contentParser;

    @Inject
    private Logger logger;

    @Inject
    private URLSecurityManager urlSecurityManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public LinkGroup()
    {
        super(NAME, DESCRIPTION,
            new DefaultContentDescriptor(CONTENT_DESCRIPTION, false, Block.LIST_BLOCK_TYPE),
            LinkGroupParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_FORMATTING));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(LinkGroupParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Get the reference parameter
        DocumentReference reference = parameters.getReference();

        // Determine the target URL
        String location = serializer.serialize(reference);
        var xwiki = contextProvider.get().getWiki();
        try {
            if (xwiki.exists(reference, contextProvider.get())) {
                location = xwiki.getURL(reference, contextProvider.get());
            }
        } catch (XWikiException e) {
            logger.error("Can't check if reference exist", e);
        }

        URI uri = null;

        // Check that we are not in spoofing attack case
        // Note this code was inspired by
        // https://github.com/xwiki/xwiki-platform/blob/8d8ce3b4fc0014ff2e255725e3958f2dc05b0ef7
        // /xwiki-platform-core/xwiki-platform-oldcore/src/main/java/com/xpn/xwiki/web/XWikiServletResponse.java#L56
        try {
            uri = urlSecurityManager.parseToSafeURI(location);
        } catch (URISyntaxException | SecurityException e) {
            logger.warn(
                "Possible phishing attack, attempting to open a link to [{}], this request has been blocked. "
                    + "If the request was legitimate, please check the URL security configuration. You "
                    + "might need to add the domain related to this request in the list of trusted domains in "
                    + "the configuration: it can be configured in xwiki.properties in url.trustedDomains.",
                location);
            logger.debug("Original error preventing to create a link to: ", e);
        }

        if (uri == null) {
            if (contextualAuthorizationManager.hasAccess(Right.EDIT)) {
                return List.of(
                    new MacroBlock("error", Map.of("title",
                        contextualLocalizationManager.getTranslationPlain(
                            "org.xwiki.contrib.link-format.invalid-uri-error.header")),
                        contextualLocalizationManager.getTranslationPlain(
                            "org.xwiki.contrib.link-format.invalid-uri-error.description", location),
                        false));
            } else {
                return List.of();
            }
        } else {
            List<Block> macroContent;
            if (StringUtils.isNotEmpty(content)) {
                macroContent =
                    contentParser.parse(content, context, false, context.isInline()).getChildren();
            } else {
                macroContent = List.of();
            }

            Syntax syntax = context.getTransformationContext().getTargetSyntax();
            SyntaxType targetSyntaxType = syntax == null ? null : syntax.getType();
            if (SyntaxType.ANNOTATED_HTML.equals(targetSyntaxType) || SyntaxType.ANNOTATED_XHTML.equals(
                targetSyntaxType))
            {
                Block macroEditableContent = new MetaDataBlock(macroContent, getNonGeneratedContentMetaData());

                // Don't mark as link in edit mode to avoid the WYSIWYG convert all content as link
                if (context.isInline()) {
                    return List.of(macroEditableContent);
                } else {
                    return List.of(new GroupBlock(List.of(macroEditableContent)));
                }
            } else {
                String htmlLinkContent = "<a href=\"" + XMLUtils.escape(location) + "\">";
                if (context.isInline()) {
                    List<Block> result = new ArrayList<>(2 + macroContent.size());
                    result.add(new RawBlock(htmlLinkContent, Syntax.HTML_5_0));
                    result.addAll(macroContent);
                    result.add(new RawBlock(END_LINK, Syntax.HTML_5_0));
                    return result;
                } else {
                    return List.of(
                        new RawBlock(htmlLinkContent, Syntax.HTML_5_0),
                        new GroupBlock(macroContent),
                        new RawBlock(END_LINK, Syntax.HTML_5_0));
                }
            }
        }
    }
}
