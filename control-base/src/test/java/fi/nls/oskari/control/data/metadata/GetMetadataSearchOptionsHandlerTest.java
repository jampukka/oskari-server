package fi.nls.oskari.control.data.metadata;

import fi.mml.map.mapwindow.service.db.InspireThemeService;
import fi.mml.map.mapwindow.service.db.InspireThemeServiceIbatisImpl;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ServiceFactory;
import fi.nls.test.control.JSONActionRouteTest;
import fi.nls.test.util.ResourceHelper;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Problems mocking the responses -> IGNORE for now
 */
@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {IOHelper.class, PropertyUtil.class, ServiceFactory.class})
public class GetMetadataSearchOptionsHandlerTest extends JSONActionRouteTest {

    final private GetMetadataSearchOptionsHandler handler = new GetMetadataSearchOptionsHandler();
    @Before
    public void setup() throws Exception{
        mockInternalServices();
    }

    @Test
    public void testHandleAction() throws Exception {

        final ActionParameters params = createActionParams();
        handler.handleAction(params);

        final JSONObject response = ResourceHelper.readJSONResource("GetMetadataSearchOptionsHandler-response.json", this);

        verifyResponseContent(response);
    }

    private void mockInternalServices() throws Exception {
        final InspireThemeService service = mock(InspireThemeServiceIbatisImpl.class);
        doReturn(
                Collections.emptyList()
        ).when(service).findAll();


        // return mocked service if a new one is created
        // classes doing this must be listed in PrepareForTest annotation
        whenNew(InspireThemeServiceIbatisImpl.class).withNoArguments().
                thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocation) throws Throwable {
                        return service;
                    }
                });

        PowerMockito.mockStatic(IOHelper.class);


        for(MetadataField field : MetadataField.values()) {
            MetadataFieldHandler handler = field.getHandler();

            final HttpURLConnection connection = mock(HttpURLConnection.class);
            when(IOHelper.getConnection(handler.getSearchURL() + handler.getPropertyName())).
                    thenReturn(connection);
            doReturn(getInputstreamForProperty(handler.getPropertyName())).when(connection).getInputStream();
        }

        /*
            when(IOHelper.getURL(handler.getSearchURL() + handler.getPropertyName())).
                    thenReturn(ResourceHelper.readStringResource(handler.getPropertyName() + "-response.json",this));
         */
    }
    private InputStream getInputstreamForProperty(final String property) {
        final String resource = ResourceHelper.readStringResource(property + "-response.json",this);
        System.out.println(property + "-response.json: " + resource);
        return new ByteArrayInputStream(resource.getBytes());
    }

    public static String readString(InputStream is, final String charset)
            throws IOException {
        /*
         * To convert the InputStream to String we use the Reader.read(char[]
         * buffer) method. We iterate until the Reader return -1 which means
         * there's no more data to read. We use the StringWriter class to
         * produce the string.
         */
        if (is == null) {
            return "";
        }
        final Writer writer = new StringWriter();
        final char[] buffer = new char[1024];
        try {
            final Reader reader = new BufferedReader(new InputStreamReader(is,
                    charset));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            is.close();
        }
        return writer.toString();
    }

}
