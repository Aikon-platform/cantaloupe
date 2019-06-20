package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.test.WebServer;
import org.junit.Before;

public class HttpSource2HTTPS1Test extends HttpSource2HTTPSTest {

    @Before
    @Override
    public void setUp() throws Exception {
        server = new WebServer();
        server.setHTTP1Enabled(false);
        server.setHTTP2Enabled(false);
        server.setHTTPS1Enabled(true);
        server.setHTTPS2Enabled(false);

        super.setUp();
    }

}
