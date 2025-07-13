package io.github.bitfist.jcef.spring.query;

import lombok.Data;

/**
 * <p>
 * Represents a JSON object used in a Chromium Embedded Framework (CEF) query.
 * This class encapsulates the route and the payload of the query.
 * </p>
 * <p>
 * The {@code route} field specifies the routing path or identifier used for handling the query.
 * It acts as a key for identifying the handler method or logic for processing the query.
 * </p>
 * <p>
 * The {@code payload} field contains the data associated with the query.
 * It can represent any information or parameters required to process the query
 * and is stored as a generic {@code Object}.
 * </p>
 */
@Data
public class CefQueryJson {
    private String route;
    private Object payload;
}
