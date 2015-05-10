/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */

package org.truelicense.jax.rs;

import org.truelicense.api.License;
import org.truelicense.api.LicenseConsumerManager;
import org.truelicense.api.LicenseManagementException;
import org.truelicense.obfuscate.Obfuscate;
import org.truelicense.spi.io.MemoryStore;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Objects;

import static javax.ws.rs.core.MediaType.*;

/**
 * A RESTful web service for license management in consumer applications.
 * This class is immutable.
 *
 * @since  TrueLicense 2.3
 * @author Christian Schlichtherle
 */
@Path("license")
@Produces({ APPLICATION_JSON, APPLICATION_XML, TEXT_XML })
public final class LicenseConsumerService {

    private static final int BAD_REQUEST_STATUS_CODE = 400;
    private static final int PAYMENT_REQUIRED_STATUS_CODE = 402;
    private static final int NOT_FOUND_STATUS_CODE = 404;

    @Obfuscate private static final String SUBJECT = "subject";

    private static final QName subject = new QName(SUBJECT);

    private final LicenseConsumerManager manager;

    /**
     * Constructs a license consumer service.
     * This constructor immediately resolves the license consumer manager by
     * looking up a {@code ContextResolver<LicenseConsumerManager>} in the
     * given providers.
     * You can provide a context resolver for this class like this:
     * <pre>{@code
     * package ...;
     *
     * import javax.jax.rs.ext.*;
     * import org.truelicense.api.LicenseConsumerManager;
     *
     * &#64;Provider
     * public class LicenseConsumerManagerResolver
     * implements ContextResolver<LicenseConsumerManager> {
     *     &#64;Override public LicenseConsumerManager getContext(Class<?> type) {
     *         return LicenseManager.get();
     *     }
     * }
     * }</pre>
     * <p>
     * where {@code LicenseManager} is your custom license manager enumeration
     * class as generated by the TrueLicense Maven Archetype.
     *
     * @param providers the providers.
     */
    public LicenseConsumerService(@Context Providers providers) {
        this(manager(providers));
    }

    private static LicenseConsumerManager manager(final Providers providers) {
        final ContextResolver<LicenseConsumerManager>
                resolver = providers.getContextResolver(LicenseConsumerManager.class, WILDCARD_TYPE);
        if (null == resolver)
            throw new IllegalArgumentException("No @Provider annotated ContextResolver<LicenseConsumerManager> available.");
        return resolver.getContext(LicenseConsumerManager.class);
    }

    /**
     * Constructs a license consumer service.
     * This is the preferable constructor with Dependency Injection frameworks,
     * e.g. CDI, Spring or Guice.
     *
     * @param manager the license consumer manager.
     */
    @Inject
    public LicenseConsumerService(final LicenseConsumerManager manager) {
        this.manager = Objects.requireNonNull(manager);
    }

    @GET
    @Path("subject")
    @Produces(APPLICATION_JSON)
    public String subjectAsJson() { return '"' + subject() + '"'; }

    @GET
    @Path("subject")
    @Produces({ APPLICATION_XML, TEXT_XML })
    public JAXBElement<String> subjectAsXml() {
        return new JAXBElement<String>(subject, String.class, subject());
    }

    @GET
    @Path("subject")
    @Produces(TEXT_PLAIN)
    public String subject() { return manager.context().subject(); }

    @POST
    public void install(final byte[] key)
    throws LicenseConsumerServiceException {
        final MemoryStore store = new MemoryStore();
        store.data(key);
        try {
            manager.install(store);
        } catch (LicenseManagementException ex) {
            throw new LicenseConsumerServiceException(BAD_REQUEST_STATUS_CODE, ex);
        }
    }

    @GET
    public License view(
            final @QueryParam("verify") @DefaultValue("false") boolean verify)
    throws LicenseConsumerServiceException {
        final License license;
        try {
            license = manager.view();
        } catch (LicenseManagementException ex) {
            throw new LicenseConsumerServiceException(NOT_FOUND_STATUS_CODE, ex);
        }
        if (verify) {
            try {
                manager.verify();
            } catch (LicenseManagementException ex) {
                throw new LicenseConsumerServiceException(PAYMENT_REQUIRED_STATUS_CODE, ex);
            }
        }
        return license;
    }

    @DELETE
    public void uninstall() throws LicenseConsumerServiceException {
        try {
            manager.uninstall();
        } catch (LicenseManagementException ex) {
            throw new LicenseConsumerServiceException(NOT_FOUND_STATUS_CODE, ex);
        }
    }
}
