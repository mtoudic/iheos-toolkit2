package gov.nist.toolkit.results.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by bill on 10/3/15.
 */
public enum LogIdType implements IsSerializable, Serializable {
    TIME_ID,     // used for utilities
    SPECIFIC_ID  // used for tests - so they overwrite the last instance
}
